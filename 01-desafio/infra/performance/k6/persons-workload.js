import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TEST_TYPE = (__ENV.TEST_TYPE || 'load').toLowerCase();
const THINK_TIME_SECONDS = Number(__ENV.THINK_TIME_SECONDS || '0.2');
const DEBUG = __ENV.DEBUG === 'true';
const SKIP_TEARDOWN = __ENV.SKIP_TEARDOWN === 'true';

const PROFILE_CONFIG = {
  smoke: {
    seedCount: 5,
    options: {
      vus: 1,
      duration: '30s',
      thresholds: {
        http_req_failed: ['rate<0.01'],
        checks: ['rate>0.99'],
        http_req_duration: ['p(95)<500', 'p(99)<1200'],
      },
    },
  },
  load: {
    seedCount: 30,
    options: {
      stages: [
        { duration: '1m', target: 10 },
        { duration: '3m', target: 10 },
        { duration: '1m', target: 0 },
      ],
      thresholds: {
        http_req_failed: ['rate<0.02'],
        checks: ['rate>0.98'],
        http_req_duration: ['p(95)<350', 'p(99)<900'],
      },
    },
  },
  stress: {
    seedCount: 60,
    options: {
      stages: [
        { duration: '1m', target: 20 },
        { duration: '2m', target: 40 },
        { duration: '2m', target: 60 },
        { duration: '2m', target: 60 },
        { duration: '1m', target: 0 },
      ],
      thresholds: {
        http_req_failed: ['rate<0.05'],
        checks: ['rate>0.95'],
        http_req_duration: ['p(95)<900', 'p(99)<2000'],
      },
    },
  },
};

if (!PROFILE_CONFIG[TEST_TYPE]) {
  fail(`TEST_TYPE invalid: ${TEST_TYPE}. Use smoke, load or stress.`);
}

const activeProfile = PROFILE_CONFIG[TEST_TYPE];
const seedCount = Number(__ENV.SEED_COUNT || activeProfile.seedCount);

export const options = {
  ...activeProfile.options,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    ...activeProfile.options.thresholds,
    'http_req_duration{operation:list}': ['p(95)<400'],
    'http_req_duration{operation:getById}': ['p(95)<400'],
    'http_req_duration{operation:create}': ['p(95)<700'],
  },
};

function correlationId(operation) {
  const vu = typeof __VU !== 'undefined' ? __VU : 0;
  const iter = typeof __ITER !== 'undefined' ? __ITER : 0;
  return `${operation}-${Date.now()}-${vu}-${iter}-${Math.floor(Math.random() * 100000)}`;
}

function requestParams(operation) {
  return {
    tags: { operation },
    headers: {
      'Content-Type': 'application/json',
      'X-Correlation-Id': correlationId(operation),
    },
  };
}

function randomBirthDate() {
  const year = 1960 + Math.floor(Math.random() * 40);
  const month = String(1 + Math.floor(Math.random() * 12)).padStart(2, '0');
  const day = String(1 + Math.floor(Math.random() * 28)).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function buildPersonPayload(suffix) {
  const unique = `${suffix}-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  return JSON.stringify({
    name: `K6 User ${unique}`,
    email: `k6_${unique}@example.com`,
    birthDate: randomBirthDate(),
  });
}

function parseJson(response) {
  try {
    return response.json();
  } catch (error) {
    return null;
  }
}

function waitForAppReady(maxAttempts = 30) {
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const response = http.get(`${BASE_URL}/actuator/health`, requestParams('health'));
    if (response.status === 200) {
      const body = parseJson(response);
      if (body && body.status === 'UP') {
        return;
      }
    }
    sleep(1);
  }
  fail(`Application at ${BASE_URL} is not ready after ${maxAttempts}s`);
}

function createPerson(suffix) {
  const response = http.post(
    `${BASE_URL}/api/persons`,
    buildPersonPayload(suffix),
    requestParams('create'),
  );

  const body = parseJson(response);
  const ok = check(response, {
    'create returned 201': (r) => r.status === 201,
    'create returned id': () => body && !!body.id,
  });

  if (!ok && DEBUG) {
    console.error(`create failed: status=${response.status} body=${response.body}`);
  }

  if (response.status !== 201 || !body || !body.id) {
    return null;
  }

  return body;
}

function listPersons() {
  const response = http.get(
    `${BASE_URL}/api/persons?page=0&size=10&sort=createdAt,desc`,
    requestParams('list'),
  );

  const body = parseJson(response);
  const ok = check(response, {
    'list returned 200': (r) => r.status === 200,
    'list content exists': () => body && Array.isArray(body.content),
  });

  if (!ok && DEBUG) {
    console.error(`list failed: status=${response.status} body=${response.body}`);
  }

  return body;
}

function getPersonById(personId) {
  const response = http.get(`${BASE_URL}/api/persons/${personId}`, requestParams('getById'));

  const ok = check(response, {
    'getById returned 200': (r) => r.status === 200,
  });

  if (!ok && DEBUG) {
    console.error(`getById failed: id=${personId} status=${response.status} body=${response.body}`);
  }

  return response.status === 200;
}

function patchPerson(personId) {
  const payload = JSON.stringify({
    name: `K6 Updated ${Date.now()}`,
  });
  const response = http.patch(
    `${BASE_URL}/api/persons/${personId}`,
    payload,
    requestParams('patch'),
  );

  const ok = check(response, {
    'patch returned 200': (r) => r.status === 200,
  });

  if (!ok && DEBUG) {
    console.error(`patch failed: id=${personId} status=${response.status} body=${response.body}`);
  }

  return response.status === 200;
}

function deletePerson(personId, allowNotFound = false) {
  const response = http.del(`${BASE_URL}/api/persons/${personId}`, null, requestParams('delete'));

  const ok = check(response, {
    'delete returned expected status': (r) => r.status === 204 || (allowNotFound && r.status === 404),
  });

  if (!ok && DEBUG) {
    console.error(`delete failed: id=${personId} status=${response.status} body=${response.body}`);
  }

  return response.status === 204 || (allowNotFound && response.status === 404);
}

function randomSeedId(seedIds) {
  const index = Math.floor(Math.random() * seedIds.length);
  return seedIds[index];
}

export function setup() {
  waitForAppReady();

  const seedIds = [];
  for (let i = 0; i < seedCount; i += 1) {
    const created = createPerson(`seed-${i}`);
    if (created && created.id) {
      seedIds.push(created.id);
    }
  }

  if (seedIds.length === 0) {
    fail('Unable to create seed data for performance test.');
  }

  return { seedIds };
}

export default function (data) {
  const roll = Math.random();

  if (roll < 0.60) {
    listPersons();
  } else if (roll < 0.90) {
    const personId = randomSeedId(data.seedIds);
    getPersonById(personId);
  } else {
    const created = createPerson(`flow-vu${__VU}-it${__ITER}`);
    if (created && created.id) {
      patchPerson(created.id);
      deletePerson(created.id);
    }
  }

  if (THINK_TIME_SECONDS > 0) {
    sleep(THINK_TIME_SECONDS);
  }
}

export function teardown(data) {
  if (SKIP_TEARDOWN) {
    return;
  }

  for (const personId of data.seedIds) {
    deletePerson(personId, true);
  }
}
