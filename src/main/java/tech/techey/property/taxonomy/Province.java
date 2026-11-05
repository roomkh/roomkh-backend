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

/** Top level of the Cambodian administrative hierarchy (MoI codes). */
@Entity
@Table(name = "provinces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Province extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 16)
    private String code;

    @Column(name = "name_km", nullable = false)
    private String nameKm;

    @Column(name = "name_en", nullable = false)
    private String nameEn;
}
