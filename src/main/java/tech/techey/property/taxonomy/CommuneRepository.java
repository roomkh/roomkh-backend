package tech.techey.property.taxonomy;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommuneRepository extends JpaRepository<Commune, Long> {

    List<Commune> findByDistrictId(Long districtId);
}
