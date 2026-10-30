package com.hoaphat.pvt.model.dto.price;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.math.BigDecimal;

public class FlexibleBigDecimalDeserializer extends JsonDeserializer<BigDecimal> {
    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) return null;
        if (token != null && token.isNumeric()) return parser.getDecimalValue();

        String raw = parser.getValueAsString();
        if (raw == null || raw.trim().isEmpty()) return null;

        try {
            return new BigDecimal(normalize(raw));
        } catch (NumberFormatException error) {
            throw JsonMappingException.from(parser,
                    "Gia khong dung dinh dang so: " + raw, error);
        }
    }

    private String normalize(String raw) {
        String value = raw.trim()
                .replace("\u00A0", "")
                .replace(" ", "")
                .replaceAll("[^0-9,.-]", "");
        int comma = value.lastIndexOf(',');
        int dot = value.lastIndexOf('.');

        if (comma >= 0 && dot >= 0) {
            char decimalSeparator = comma > dot ? ',' : '.';
            char groupingSeparator = decimalSeparator == ',' ? '.' : ',';
            value = value.replace(String.valueOf(groupingSeparator), "");
            if (decimalSeparator == ',') value = value.replace(',', '.');
            return value;
        }

        char separator = comma >= 0 ? ',' : dot >= 0 ? '.' : '\0';
        if (separator == '\0') return value;

        int first = value.indexOf(separator);
        int last = value.lastIndexOf(separator);
        int digitsAfter = value.length() - last - 1;
        boolean grouping = first != last || digitsAfter == 3;
        if (grouping) return value.replace(String.valueOf(separator), "");
        return separator == ',' ? value.replace(',', '.') : value;
    }
}
