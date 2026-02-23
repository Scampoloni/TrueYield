package ch.zhaw.trueyield.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

@RestController
public class MongoTestController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping("/api/testmongodb")
    public ResponseEntity<String> testMongoDb() {
        String failure;
        try {
            // 1. Test-Dokument erstellen
            Long time = System.currentTimeMillis();
            DBObject objectToSave = BasicDBObjectBuilder.start().add("time", time).get();

            // 2. In der Collection "Test" speichern [cite: 165]
            mongoTemplate.save(objectToSave, "Test");

            // 3. Dokument anhand der ID wieder auslesen [cite: 167]
            DBObject read = mongoTemplate.findById(objectToSave.get("_id"), DBObject.class, "Test");

            // 4. Überprüfen, ob es geklappt hat [cite: 169]
            if (read != null && read.get("time").toString().equals(time.toString())) {
                return new ResponseEntity<>("Connection ok", HttpStatus.OK);
            }
            failure = "Retrieved document does not match";
        } catch (Exception e) {
            failure = e.toString();
        }
        return new ResponseEntity<>("Failed: " + failure, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}