package app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        //Read .json data
        InputStream person1IS = Main.class.getClassLoader().getResourceAsStream("person1.json");
        InputStream person2IS = Main.class.getClassLoader().getResourceAsStream("person2.json");
        InputStream durationIS = Main.class.getClassLoader().getResourceAsStream("duration.json");

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode person1JsonNode = objectMapper.readValue(person1IS, JsonNode.class);
        JsonNode person2JsonNode = objectMapper.readValue(person2IS, JsonNode.class);
        JsonNode durationJsonNode = objectMapper.readValue(durationIS, JsonNode.class);

        final int duration = durationJsonNode.get("duration").asInt();
        ReservedHours person1WorkingHours = parseReservedHours(person1JsonNode);
        ReservedHours person2WorkingHours = parseReservedHours(person2JsonNode);

        List<ReservedHours> person1ReservedHours = parsePlannedMeeting(person1JsonNode);
        List<ReservedHours> person2ReservedHours = parsePlannedMeeting(person2JsonNode);

        person1ReservedHours.addAll(person2ReservedHours);

        LocalTime startTime;
        LocalTime endTime;

        if (person1WorkingHours.getStart().isAfter(person2WorkingHours.getStart())) {
            startTime = person1WorkingHours.getStart();
        } else {
            startTime = person2WorkingHours.getStart();
        }

        if (person1WorkingHours.getEnd().isBefore(person1WorkingHours.getEnd())) {
            endTime = person1WorkingHours.getEnd();
        } else {
            endTime = person2WorkingHours.getEnd();
        }

        person1ReservedHours.add(new ReservedHours(startTime.minusMinutes(1), startTime));
        person1ReservedHours.add(new ReservedHours(endTime, endTime.plusMinutes(1)));

        List<List<String>> resultList = new ArrayList<>();

        person1ReservedHours.sort(new Comparator<ReservedHours>() {
            @Override
            public int compare(ReservedHours o1, ReservedHours o2) {
                if (o1.getStart().isBefore(o2.getStart())) {
                    return -1;
                } else if (o1.getStart().equals(o2.getStart()) && o1.getEnd().isBefore(o2.getEnd())) {
                    return -1;
                } else if (o1.getStart().equals(o2.getStart()) && o1.getEnd().equals(o2.getEnd())) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });

        for (int i = 0; i < person1ReservedHours.size() - 1; i++) {
            ReservedHours o1 = person1ReservedHours.get(i);
            ReservedHours o2 = person1ReservedHours.get(i+1);

            if (o1.getEnd().plusMinutes(duration).equals(o2.getStart()) || o1.getEnd().plusMinutes(duration).isBefore(o2.getStart())) {
                resultList.add(Arrays.asList(o1.getEnd().toString(), o2.getStart().toString()));
            }
        }

        File outputFile = new File("output.json");
        objectMapper.writeValue(outputFile, resultList);
    }

    public static ReservedHours parseReservedHours(JsonNode personJsonNode) {
        JsonNode working_hours = personJsonNode.get("working_hours");
        return new ReservedHours(
                LocalTime.parse(working_hours.get("start").asText()),
                LocalTime.parse(working_hours.get("end").asText())
        );
    }

    public static List<ReservedHours> parsePlannedMeeting(JsonNode personJsonNode) {
        List<ReservedHours> reservedHours = new ArrayList<>();
        ArrayNode planned_meeting = (ArrayNode) personJsonNode.get("planned_meeting");
        planned_meeting.forEach(node -> {
            reservedHours.add(new ReservedHours(
                    LocalTime.parse(node.get("start").asText()),
                    LocalTime.parse(node.get("end").asText())
            ));
        });
        return reservedHours;
    }
}
