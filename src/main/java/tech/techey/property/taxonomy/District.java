package tech.techey.property.taxonomy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.techey.property.common.audit.BaseEntity;

/** District / Khan (ស្រុក/ខណ្ឌ) — second level, child of {@link Province}. */
@Entity
@Table(name = "districts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @Column(name = "code", nullable = false, unique = true, length = 16)
    private String code;

    @Column(name = "name_km", nullable = false)
    private String nameKm;

    @Column(name = "name_en", nullable = false)
    private String nameEn;
}
