package repository

import (
	"github.com/wesleyosantos91/api-person-go-gin/internal/model"
	"gorm.io/gorm"
)

type PersonRepository struct {
	db *gorm.DB
}

func NewPersonRepository(db *gorm.DB) *PersonRepository {
	return &PersonRepository{db: db}
}

func (r *PersonRepository) FindAll() ([]model.Person, error) {
	var persons []model.Person
	result := r.db.Find(&persons)
	return persons, result.Error
}

func (r *PersonRepository) FindByID(id uint) (*model.Person, error) {
	var person model.Person
	result := r.db.First(&person, id)
	if result.Error != nil {
		return nil, result.Error
	}
	return &person, nil
}

func (r *PersonRepository) Create(person *model.Person) error {
	return r.db.Create(person).Error
}

func (r *PersonRepository) Update(person *model.Person) error {
	return r.db.Save(person).Error
}

func (r *PersonRepository) Delete(id uint) error {
	return r.db.Delete(&model.Person{}, id).Error
}
