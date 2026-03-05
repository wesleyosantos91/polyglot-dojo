package main

import (
	"log"

	"github.com/wesleyosantos91/api-person-go-gin/internal/config"
	"github.com/wesleyosantos91/api-person-go-gin/internal/model"
	"github.com/wesleyosantos91/api-person-go-gin/internal/router"
)

func main() {

	cfg := config.Load()

	db, err := config.NewDB(cfg)
	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}

	if err := db.AutoMigrate(&model.Person{}); err != nil {
		log.Fatalf("failed to migrate database: %v", err)
	}

	r := router.Setup(db)

	log.Printf("starting server on :%s", cfg.ServerPort)
	if err := r.Run(":" + cfg.ServerPort); err != nil {
		log.Fatalf("failed to start server: %v", err)
	}
}
