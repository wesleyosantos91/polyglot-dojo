package model

import "time"

type Person struct {
	ID        uint      `json:"id" gorm:"primaryKey;autoIncrement"`
	Name      string    `json:"name" gorm:"type:varchar(100);not null"`
	Email     string    `json:"email" gorm:"type:varchar(150);uniqueIndex;not null"`
	BirthDate time.Time `json:"birth_date" gorm:"type:date;not null"`
	CreatedAt time.Time `json:"created_at" gorm:"autoCreateTime"`
	UpdatedAt time.Time `json:"updated_at" gorm:"autoUpdateTime"`
}

type PersonRequest struct {
	Name      string `json:"name" binding:"required"`
	Email     string `json:"email" binding:"required,email"`
	BirthDate string `json:"birth_date" binding:"required"`
}

type PersonResponse struct {
	ID        uint   `json:"id"`
	Name      string `json:"name"`
	Email     string `json:"email"`
	BirthDate string `json:"birth_date"`
	CreatedAt string `json:"created_at"`
	UpdatedAt string `json:"updated_at"`
}
