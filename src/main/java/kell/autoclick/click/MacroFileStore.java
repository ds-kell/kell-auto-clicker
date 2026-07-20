package kell.autoclick.click;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kell.autoclick.contants.GlobalConst;
import kell.autoclick.model.Item;
import kell.autoclick.model.ItemClick;
import kell.autoclick.model.ItemComment;
import kell.autoclick.model.ItemDAD;
import kell.autoclick.model.ItemPress;

import static kell.autoclick.click.AutoClickerConfig.*;

final class MacroFileStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    void save(List<Item> items, Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            var arr = GSON.toJsonTree(items.stream().map(this::itemToJson).toList());
            GSON.toJson(arr, writer);
        }
    }

    List<Item> load(File file) throws IOException {
        String fname = file.getName().toLowerCase();
        return fname.endsWith(JSON_EXTENSION) ? parseJson(file) : parseLegacyTxt(file);
    }

    private JsonObject itemToJson(Item item) {
        var json = new JsonObject();
        json.addProperty(JSON_FIELD_TYPE, item.getTypeEvent());
        json.addProperty(JSON_FIELD_NAME, item.getName());
        json.addProperty(JSON_FIELD_PRESS_TIME, item.getPressTime());
        json.addProperty(JSON_FIELD_NEXT_TIME, item.getNextTime());
        if (item.isRecorded()) {
            json.addProperty(JSON_FIELD_RECORDED, true);
        }
        switch (item) {
            case ItemClick click -> writeClick(json, click);
            case ItemPress press -> writePress(json, press);
            case ItemDAD dad -> writeDad(json, dad);
            default -> { /* base fields are enough */ }
        }
        return json;
    }

    private void writeClick(JsonObject json, ItemClick item) {
        json.addProperty(JSON_FIELD_X, item.getXAxis());
        json.addProperty(JSON_FIELD_Y, item.getYAxis());
        json.addProperty(JSON_FIELD_MOUSE_BUTTON, item.getButton());
        if (item.getMouseAction() != null) {
            json.addProperty(JSON_FIELD_MOUSE_ACTION, item.getMouseAction());
        }
        if (item.hasRecordedScreen()) {
            json.addProperty(JSON_FIELD_SCREEN_ID, item.getRecordedScreenId());
            json.addProperty(JSON_FIELD_SCREEN_INDEX, item.getRecordedScreenIndex());
            json.addProperty(JSON_FIELD_SCREEN_X, item.getRecordedScreenX());
            json.addProperty(JSON_FIELD_SCREEN_Y, item.getRecordedScreenY());
            json.addProperty(JSON_FIELD_SCREEN_WIDTH, item.getRecordedScreenWidth());
            json.addProperty(JSON_FIELD_SCREEN_HEIGHT, item.getRecordedScreenHeight());
        }
    }

    private void writePress(JsonObject json, ItemPress item) {
        json.addProperty(JSON_FIELD_KEY_CODE, item.getKeyCode());
        json.addProperty(JSON_FIELD_MULTI_PRESS, item.isMultiPress());
        if (item.getKeyAction() != null) {
            json.addProperty(JSON_FIELD_KEY_ACTION, item.getKeyAction());
        }
    }

    private void writeDad(JsonObject json, ItemDAD item) {
        json.addProperty(JSON_FIELD_X1, item.getXAxis1());
        json.addProperty(JSON_FIELD_Y1, item.getYAxis1());
        json.addProperty(JSON_FIELD_X2, item.getXAxis2());
        json.addProperty(JSON_FIELD_Y2, item.getYAxis2());
    }

    private List<Item> parseJson(File file) throws IOException {
        var items = new ArrayList<Item>();
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            var arr = JsonParser.parseReader(reader).getAsJsonArray();
            for (var element : arr) {
                items.add(jsonToItem(element.getAsJsonObject()));
            }
        }
        return items;
    }

    private Item jsonToItem(JsonObject json) {
        String type = resolveType(json);
        String name = json.get(JSON_FIELD_NAME).getAsString();
        int pressTime = getInt(json, JSON_FIELD_PRESS_TIME, DEFAULT_PRESS_TIME);
        int nextTime = getInt(json, JSON_FIELD_NEXT_TIME, DEFAULT_NEXT_TIME);

        Item item = switch (type) {
            case GlobalConst.EVENT_CLICK -> jsonToClick(json, name, pressTime, nextTime);
            case GlobalConst.EVENT_PRESS -> jsonToPress(json, name, pressTime, nextTime);
            case GlobalConst.EVENT_DAD -> jsonToDad(json, pressTime, nextTime);
            case GlobalConst.EVENT_COMMENT -> new ItemComment(name);
            default -> new Item(name, pressTime, nextTime, type);
        };
        item.setRecorded(json.has(JSON_FIELD_RECORDED) && json.get(JSON_FIELD_RECORDED).getAsBoolean());
        return item;
    }

    private ItemClick jsonToClick(JsonObject json, String name, int pressTime, int nextTime) {
        String button = json.has(JSON_FIELD_MOUSE_BUTTON)
                ? json.get(JSON_FIELD_MOUSE_BUTTON).getAsString()
                : "Left";
        var item = new ItemClick(
                name,
                json.get(JSON_FIELD_X).getAsInt(),
                json.get(JSON_FIELD_Y).getAsInt(),
                pressTime,
                nextTime,
                button
        );
        if (json.has(JSON_FIELD_MOUSE_ACTION)) {
            item.setMouseAction(json.get(JSON_FIELD_MOUSE_ACTION).getAsString());
        }
        if (hasRecordedScreen(json)) {
            item.setRecordedScreen(
                    json.has(JSON_FIELD_SCREEN_ID) ? json.get(JSON_FIELD_SCREEN_ID).getAsString() : null,
                    getInt(json, JSON_FIELD_SCREEN_INDEX, -1),
                    getInt(json, JSON_FIELD_SCREEN_X, 0),
                    getInt(json, JSON_FIELD_SCREEN_Y, 0),
                    json.get(JSON_FIELD_SCREEN_WIDTH).getAsInt(),
                    json.get(JSON_FIELD_SCREEN_HEIGHT).getAsInt()
            );
        }
        return item;
    }

    private static boolean hasRecordedScreen(JsonObject json) {
        return json.has(JSON_FIELD_SCREEN_WIDTH)
                && json.has(JSON_FIELD_SCREEN_HEIGHT)
                && json.get(JSON_FIELD_SCREEN_WIDTH).getAsInt() > 0
                && json.get(JSON_FIELD_SCREEN_HEIGHT).getAsInt() > 0;
    }

    private ItemPress jsonToPress(JsonObject json, String name, int pressTime, int nextTime) {
        boolean multiPress = json.has(JSON_FIELD_MULTI_PRESS)
                && json.get(JSON_FIELD_MULTI_PRESS).getAsBoolean();
        var item = new ItemPress(
                name,
                json.get(JSON_FIELD_KEY_CODE).getAsInt(),
                pressTime,
                nextTime,
                multiPress
        );
        if (json.has(JSON_FIELD_KEY_ACTION)) {
            item.setKeyAction(json.get(JSON_FIELD_KEY_ACTION).getAsString());
        }
        return item;
    }

    private ItemDAD jsonToDad(JsonObject json, int pressTime, int nextTime) {
        return new ItemDAD(
                json.get(JSON_FIELD_X1).getAsInt(),
                json.get(JSON_FIELD_Y1).getAsInt(),
                json.get(JSON_FIELD_X2).getAsInt(),
                json.get(JSON_FIELD_Y2).getAsInt(),
                pressTime,
                nextTime
        );
    }

    private static int getInt(JsonObject json, String fieldName, int fallback) {
        return json.has(fieldName) ? json.get(fieldName).getAsInt() : fallback;
    }

    private List<Item> parseLegacyTxt(File file) throws IOException {
        var items = new ArrayList<Item>();
        try (var reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLegacyLine(line).ifPresent(items::add);
            }
        }
        return items;
    }

    private Optional<Item> parseLegacyLine(String rawLine) {
        String line = rawLine.trim();
        if (line.isEmpty()) {
            return Optional.empty();
        }
        if (line.startsWith(GlobalConst.COMMENT_PREFIX)) {
            return Optional.of(new ItemComment(line));
        }

        var parts = line.split("\\|");
        if (parts.length < 2) {
            return Optional.empty();
        }

        String type = legacyTypeToString(parts[0].trim());
        if (type == null) {
            return Optional.empty();
        }
        return legacyPartsToItem(parts, type);
    }

    private Optional<Item> legacyPartsToItem(String[] parts, String type) {
        String name = parts[1];
        int pressTime = parts.length > 2 ? parseLegacyInt(parts[2]) : DEFAULT_PRESS_TIME;
        int nextTime = parts.length > 3 ? parseLegacyInt(parts[3]) : DEFAULT_NEXT_TIME;

        return switch (type) {
            case GlobalConst.EVENT_CLICK -> legacyClick(parts, name, pressTime, nextTime);
            case GlobalConst.EVENT_PRESS -> legacyPress(parts, name, pressTime, nextTime);
            case GlobalConst.EVENT_DAD -> legacyDad(parts, pressTime, nextTime);
            default -> Optional.of(new Item(name, pressTime, nextTime, type));
        };
    }

    private Optional<Item> legacyClick(String[] parts, String name, int pressTime, int nextTime) {
        if (parts.length < 6) {
            return Optional.empty();
        }
        return Optional.of(new ItemClick(
                name,
                parseLegacyInt(parts[4]),
                parseLegacyInt(parts[5]),
                pressTime,
                nextTime
        ));
    }

    private Optional<Item> legacyPress(String[] parts, String name, int pressTime, int nextTime) {
        if (parts.length < 6) {
            return Optional.empty();
        }
        return Optional.of(new ItemPress(
                name,
                parseLegacyInt(parts[4]),
                pressTime,
                nextTime,
                Boolean.parseBoolean(parts[5])
        ));
    }

    private Optional<Item> legacyDad(String[] parts, int pressTime, int nextTime) {
        if (parts.length < 8) {
            return Optional.empty();
        }
        return Optional.of(new ItemDAD(
                parseLegacyInt(parts[4]),
                parseLegacyInt(parts[5]),
                parseLegacyInt(parts[6]),
                parseLegacyInt(parts[7]),
                pressTime,
                nextTime
        ));
    }

    private static String legacyTypeToString(String raw) {
        return switch (raw) {
            case "1" -> GlobalConst.EVENT_CLICK;
            case "2" -> GlobalConst.EVENT_PRESS;
            case "3" -> GlobalConst.EVENT_SCROLL;
            case "4" -> GlobalConst.EVENT_DAD;
            case "5" -> GlobalConst.EVENT_COMMENT;
            default -> null;
        };
    }

    private static String resolveType(JsonObject json) {
        var element = json.get(JSON_FIELD_TYPE);
        if (element == null) {
            return "";
        }
        if (element.getAsJsonPrimitive().isNumber()) {
            return switch (element.getAsInt()) {
                case 1 -> GlobalConst.EVENT_CLICK;
                case 2 -> GlobalConst.EVENT_PRESS;
                case 3 -> GlobalConst.EVENT_SCROLL;
                case 4 -> GlobalConst.EVENT_DAD;
                case 5 -> GlobalConst.EVENT_COMMENT;
                default -> "";
            };
        }
        return element.getAsString();
    }

    private int parseLegacyInt(String value) {
        return AutoClickerUtils.parseInt(value, -1);
    }
}
