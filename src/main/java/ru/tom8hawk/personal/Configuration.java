package ru.tom8hawk.personal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class Configuration {
    private String token;
    @JsonDeserialize(using = ListToStringDeserializer.class)
    private String startMessage;
    @JsonDeserialize(using = ListToStringDeserializer.class)
    private String exampleRequestMessage;

    public static Configuration init() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try {
            File target = new File("config.yml");

            if (!target.exists()) {
                try (InputStream inputStream = Graphs.class.getClassLoader().getResourceAsStream("config.yml");
                     OutputStream outputStream = new FileOutputStream(target)) {

                    inputStream.transferTo(outputStream);
                }
            }

            return mapper.readValue(target, Configuration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ListToStringDeserializer extends StdDeserializer<String> {
        protected ListToStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return (String) parser.readValueAs(List.class).stream().map(String::valueOf).collect(Collectors.joining("\n"));
        }
    }
}