package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.zolhungaj.amqcontestbot.database.model.InternationalisationEntity;

import java.util.List;

@Repository
public interface InternationalisationRepository extends JpaRepository<InternationalisationEntity, Integer>{
    List<InternationalisationEntity> findAllByCanonicalNameIsAndLanguageCodeIsAndSubLanguageIs(String canonicalName, String languageCode, String subLanguage);
    List<InternationalisationEntity> findAllByCanonicalNameIsAndLanguageCodeIs(String canonicalName, String languageCode);
}
