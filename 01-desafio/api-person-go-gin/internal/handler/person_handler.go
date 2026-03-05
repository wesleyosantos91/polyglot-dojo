package handler

import (
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/wesleyosantos91/api-person-go-gin/internal/model"
	"github.com/wesleyosantos91/api-person-go-gin/internal/repository"
)

type PersonHandler struct {
	repo *repository.PersonRepository
}

func NewPersonHandler(repo *repository.PersonRepository) *PersonHandler {
	return &PersonHandler{repo: repo}
}

func (h *PersonHandler) FindAll(c *gin.Context) {
	persons, err := h.repo.FindAll()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	response := make([]model.PersonResponse, len(persons))
	for i, p := range persons {
		response[i] = toResponse(p)
	}

	c.JSON(http.StatusOK, response)
}

func (h *PersonHandler) FindByID(c *gin.Context) {
	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	person, err := h.repo.FindByID(uint(id))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "person not found"})
		return
	}

	c.JSON(http.StatusOK, toResponse(*person))
}

func (h *PersonHandler) Create(c *gin.Context) {
	var req model.PersonRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	birthDate, err := time.Parse("2006-01-02", req.BirthDate)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid birth_date format, expected YYYY-MM-DD"})
		return
	}

	person := model.Person{
		Name:      req.Name,
		Email:     req.Email,
		BirthDate: birthDate,
	}

	if err := h.repo.Create(&person); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, toResponse(person))
}

func (h *PersonHandler) Update(c *gin.Context) {
	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	person, err := h.repo.FindByID(uint(id))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "person not found"})
		return
	}

	var req model.PersonRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	birthDate, err := time.Parse("2006-01-02", req.BirthDate)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid birth_date format, expected YYYY-MM-DD"})
		return
	}

	person.Name = req.Name
	person.Email = req.Email
	person.BirthDate = birthDate

	if err := h.repo.Update(person); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, toResponse(*person))
}

func (h *PersonHandler) Delete(c *gin.Context) {
	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	if _, err := h.repo.FindByID(uint(id)); err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "person not found"})
		return
	}

	if err := h.repo.Delete(uint(id)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.Status(http.StatusNoContent)
}

func toResponse(p model.Person) model.PersonResponse {
	return model.PersonResponse{
		ID:        p.ID,
		Name:      p.Name,
		Email:     p.Email,
		BirthDate: p.BirthDate.Format("2006-01-02"),
		CreatedAt: p.CreatedAt.Format(time.RFC3339),
		UpdatedAt: p.UpdatedAt.Format(time.RFC3339),
	}
}
