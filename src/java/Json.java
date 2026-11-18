import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private final String text;
    private int position;

    private Json(String text) { this.text = text; }

    public static Object parse(String text) {
        Json reader = new Json(text);
        Object value = reader.value();
        reader.space();
        if (reader.position != text.length()) throw reader.error();
        return value;
    }
    private IllegalArgumentException error() {
        return new IllegalArgumentException("Invalid JSON at offset " + position);
    }
    private void space() {
        while (position < text.length() && " \r\n\t".indexOf(text.charAt(position)) >= 0) position++;
    }
    private boolean take(char symbol) {
        space();
        if (position == text.length() || text.charAt(position) != symbol) return false;
        position++;
        return true;
    }
    private void expect(char symbol) { if (!take(symbol)) throw error(); }
    private Object value() {
        space();
        if (position >= text.length()) throw error();
        char first = text.charAt(position);
        if (first == '"') return string();
        if (first == '{') {
            position++;
            Map<String, Object> values = new LinkedHashMap<>();
            if (take('}')) return values;
            do {
                space();
                if (position >= text.length() || text.charAt(position) != '"') throw error();
                String key = string();
                expect(':');
                if (values.containsKey(key)) throw error();
                values.put(key, value());
            } while (take(','));
            expect('}');
            return values;
        }
        if (first == '[') {
            position++;
            List<Object> values = new ArrayList<>();
            if (take(']')) return values;
            do { values.add(value()); } while (take(','));
            expect(']');
            return values;
        }
        for (String literal : List.of("true", "false", "null"))
            if (text.startsWith(literal, position)) {
                position += literal.length();
                return literal.equals("null") ? null : Boolean.valueOf(literal);
            }
        int start = position;
        while (position < text.length() && "-+0123456789.eE".indexOf(text.charAt(position)) >= 0) position++;
        String number = text.substring(start, position);
        if (!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) throw error();
        BigDecimal result = new BigDecimal(number);
        if (!Double.isFinite(result.doubleValue())) throw error();
        return result;
    }
    private String string() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (position < text.length()) {
            char symbol = text.charAt(position++);
            if (symbol == '"') return result.toString();
            if (symbol < 32) throw error();
            if (symbol != '\\') {
                result.append(symbol);
                continue;
            }
            if (position >= text.length()) throw error();
            char escape = text.charAt(position++);
            switch (escape) {
                case '"', '\\', '/' -> result.append(escape);
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> {
                    if (position + 4 > text.length()) throw error();
                    try { result.append((char) Integer.parseInt(text.substring(position, position + 4), 16)); }
                    catch (NumberFormatException exception) { throw error(); }
                    position += 4;
                }
                default -> throw error();
            }
        }
        throw error();
    }
    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String string) {
            StringBuilder result = new StringBuilder("\"");
            for (char symbol : string.toCharArray())
                switch (symbol) {
                    case '"' -> result.append("\\\"");
                    case '\\' -> result.append("\\\\");
                    default -> {
                        if (symbol < 32) result.append(String.format("\\u%04x", (int) symbol));
                        else result.append(symbol);
                    }
                }
            return result.append('"').toString();
        }
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            for (var entry : map.entrySet())
                entries.add(stringify(entry.getKey().toString()) + ": " + stringify(entry.getValue()));
            return "{" + String.join(", ", entries) + "}";
        }
        if (value instanceof List<?> list)
            return "[" + String.join(", ", list.stream().map(Json::stringify).toList()) + "]";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        throw new IllegalArgumentException("Unsupported JSON value");
    }
}
