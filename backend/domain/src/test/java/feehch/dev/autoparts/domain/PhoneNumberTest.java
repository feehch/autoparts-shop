package feehch.dev.autoparts.domain;

import feehch.dev.autoparts.domain.common.DomainException;
import feehch.dev.autoparts.domain.common.PhoneNumber;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"+375291234567", "+375 (29) 123-45-67", "375291234567", "8 029 123 45 67", "80291234567"})
    void acceptsBelarusianFormats(String raw) {
        assertThat(PhoneNumber.normalize(raw)).isEqualTo("+375291234567");
    }

    @Test
    void acceptsMinskLandline() {
        assertThat(PhoneNumber.normalize("+375 17 222-33-44")).isEqualTo("+375172223344");
    }

    @Test
    void blankMeansNoPhone() {
        assertThat(PhoneNumber.normalize("  ")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"+79990000000", "+375991234567", "+37529123456", "+3752912345678", "abc", "+375 29 123 45 6x"})
    void rejectsInvalidNumbers(String raw) {
        assertThatThrownBy(() -> PhoneNumber.normalize(raw)).isInstanceOf(DomainException.class);
    }
}
