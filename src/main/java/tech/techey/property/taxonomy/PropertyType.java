package tech.techey.property.taxonomy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.techey.property.common.audit.BaseEntity;

/** Property type taxonomy (Room, Apartment, Villa, ...) per PRD §8.1. */
@Entity
@Table(name = "property_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyType extends BaseEntity {

    @Column(name = "key", nullable = false, unique = true, length = 32)
    private String key;

    @Column(name = "name_km", nullable = false)
    private String nameKm;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false, length = 8)
    private AppliesTo appliesTo;

    @Column(name = "icon")
    private String icon;

    public enum AppliesTo {
        RENT,
        SALE,
        BOTH
    }
}
