package tech.techey.property.taxonomy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.techey.property.common.audit.BaseEntity;

/** Listing amenity taxonomy (Aircon, Wi-Fi, Parking, ...) per PRD §8.1. */
@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amenity extends BaseEntity {

    @Column(name = "key", nullable = false, unique = true, length = 32)
    private String key;

    @Column(name = "name_km", nullable = false)
    private String nameKm;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "icon")
    private String icon;

    @Column(name = "category", length = 32)
    private String category;
}
