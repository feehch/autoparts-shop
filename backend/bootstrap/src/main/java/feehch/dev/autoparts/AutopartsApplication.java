package feehch.dev.autoparts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа приложения «Интернет-магазин автозапчастей».
 * Сканирование компонентов охватывает пакеты всех модулей ({@code feehch.dev.autoparts.*}).
 */
@SpringBootApplication
public class AutopartsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutopartsApplication.class, args);
    }
}
