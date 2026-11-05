package tech.techey.property.taxonomy;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VillageRepository extends JpaRepository<Village, Long> {

    List<Village> findByCommuneId(Long communeId);
}
