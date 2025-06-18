package com.tiembanhngot.tiem_banh_online.entity;

import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString; 

@Entity
@Getter
@Setter
@Table(name = "categories")
@ToString(exclude = "products") 
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(length = 100, unique = true, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private Set<Product> products;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return categoryId != null && Objects.equals(categoryId, category.categoryId); // so sanh va tim kiem bang categoryID
    }

    @Override
    public int hashCode() {
        if (categoryId != null){
            return Objects.hash(categoryId);
        }
        else return System.identityHashCode(this);
    }
}