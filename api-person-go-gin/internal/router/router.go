package router

import (
	"github.com/gin-gonic/gin"
	"github.com/wesleyosantos91/api-person-go-gin/internal/handler"
	"github.com/wesleyosantos91/api-person-go-gin/internal/repository"
	"gorm.io/gorm"
)

func Setup(db *gorm.DB) *gin.Engine {

	r := gin.Default()

	personRepo := repository.NewPersonRepository(db)
	personHandler := handler.NewPersonHandler(personRepo)

	api := r.Group("/api")
	{
		persons := api.Group("/persons")
		{
			persons.GET("", personHandler.FindAll)
			persons.GET("/:id", personHandler.FindByID)
			persons.POST("", personHandler.Create)
			persons.PUT("/:id", personHandler.Update)
			persons.DELETE("/:id", personHandler.Delete)
		}
	}

	return r
}
