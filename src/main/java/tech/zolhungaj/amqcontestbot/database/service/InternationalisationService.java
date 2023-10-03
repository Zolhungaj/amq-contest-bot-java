package tech.zolhungaj.amqcontestbot.database.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.model.InternationalisationEntity;
import tech.zolhungaj.amqcontestbot.database.repository.InternationalisationRepository;

import java.util.List;
import java.util.random.RandomGenerator;

@Service
@Slf4j
@RequiredArgsConstructor
public class InternationalisationService {

    private final InternationalisationRepository repository;
    private final RandomGenerator randomGenerator = RandomGenerator.of("L64X128MixRandom");

    public String getMessage(String i18nCanonicalName, String languageCode, String subLanguage, List<Object> arguments){
        log.debug("Start replacement {}", i18nCanonicalName);
        String message = getMessageForCanonicalName(i18nCanonicalName, languageCode, subLanguage);
        for(var i = 0; i < arguments.size(); i++){
            String search = "%" + (i+1) + "%";
            String argument = String.valueOf(arguments.get(i))
                    .replace("\\", "\\\\")
                    .replace("$", "\\$");
            message = message.replaceAll(search, argument);
        }
        return message;
    }
    private String getMessageForCanonicalName(String i18nCanonicalName, String languageCode, String subLanguage){
        List<InternationalisationEntity> firstChoice = repository.findAllByCanonicalNameIsAndLanguageCodeIsAndSubLanguageIs(i18nCanonicalName, languageCode, subLanguage);
        if(!firstChoice.isEmpty()){
            return getRandomEntry(firstChoice);
        }
        List<InternationalisationEntity> fallback = repository.findAllByCanonicalNameIsAndLanguageCodeIs(i18nCanonicalName, languageCode);
        if(!fallback.isEmpty()){
            return getRandomEntry(fallback);
        }
        List<InternationalisationEntity> fallbackEnglish = repository.findAllByCanonicalNameIsAndLanguageCodeIs(i18nCanonicalName, "en");
        if(!fallbackEnglish.isEmpty()){
            return getRandomEntry(fallbackEnglish);
        }
        return "ERROR missing message: " + i18nCanonicalName + "%1% %2% %3% %4% %5% %6% %7%";
    }

    private String getRandomEntry(List<InternationalisationEntity> entries){
        return entries.get(randomGenerator.nextInt(entries.size())).getContent();
    }

    public String censor(String message){
        return message.replace("ROBOT", "MACHINE");//TODO:implement censoring preventing certain phrases from being spoken, once database is done
    }
}
