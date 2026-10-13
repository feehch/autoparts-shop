package feehch.dev.autoparts.bootstrap;

import feehch.dev.autoparts.application.auth.AuthService;
import feehch.dev.autoparts.application.cart.CartService;
import feehch.dev.autoparts.application.catalog.CatalogService;
import feehch.dev.autoparts.application.catalog.ProductService;
import feehch.dev.autoparts.application.order.OrderService;
import feehch.dev.autoparts.application.port.out.BrandRepository;
import feehch.dev.autoparts.application.port.out.CarMakeRepository;
import feehch.dev.autoparts.application.port.out.CarModelRepository;
import feehch.dev.autoparts.application.port.out.CartRepository;
import feehch.dev.autoparts.application.port.out.CategoryRepository;
import feehch.dev.autoparts.application.port.out.OrderRepository;
import feehch.dev.autoparts.application.port.out.PasswordHasher;
import feehch.dev.autoparts.application.port.out.ProductRepository;
import feehch.dev.autoparts.application.port.out.TokenProvider;
import feehch.dev.autoparts.application.port.out.TokenStore;
import feehch.dev.autoparts.application.port.out.UnitOfWork;
import feehch.dev.autoparts.application.port.out.UserRepository;
import feehch.dev.autoparts.application.stats.StatsService;
import feehch.dev.autoparts.application.user.ProfileService;
import feehch.dev.autoparts.application.user.UserAdministrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Composition root: сценарии использования прикладного слоя не знают о Spring,
 * поэтому они регистрируются как бины здесь, с внедрением реализаций портов из инфраструктуры.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public AuthService authService(UserRepository users, PasswordHasher hasher, TokenProvider tokenProvider,
                                   TokenStore tokenStore, UnitOfWork unitOfWork, Clock clock) {
        return new AuthService(users, hasher, tokenProvider, tokenStore, unitOfWork, clock);
    }

    @Bean
    public ProfileService profileService(UserRepository users, PasswordHasher hasher, UnitOfWork unitOfWork) {
        return new ProfileService(users, hasher, unitOfWork);
    }

    @Bean
    public UserAdministrationService userAdministrationService(UserRepository users, PasswordHasher hasher,
                                                               TokenStore tokenStore, UnitOfWork unitOfWork,
                                                               Clock clock) {
        return new UserAdministrationService(users, hasher, tokenStore, unitOfWork, clock);
    }

    @Bean
    public CatalogService catalogService(CategoryRepository categories, BrandRepository brands,
                                         CarMakeRepository makes, CarModelRepository models,
                                         ProductRepository products, UnitOfWork unitOfWork) {
        return new CatalogService(categories, brands, makes, models, products, unitOfWork);
    }

    @Bean
    public ProductService productService(ProductRepository products, CategoryRepository categories,
                                         BrandRepository brands, CarMakeRepository makes,
                                         CarModelRepository models, UnitOfWork unitOfWork, Clock clock) {
        return new ProductService(products, categories, brands, makes, models, unitOfWork, clock);
    }

    @Bean
    public CartService cartService(CartRepository carts, ProductRepository products, UnitOfWork unitOfWork) {
        return new CartService(carts, products, unitOfWork);
    }

    @Bean
    public OrderService orderService(OrderRepository orders, CartRepository carts, ProductRepository products,
                                     UserRepository users, UnitOfWork unitOfWork, Clock clock) {
        return new OrderService(orders, carts, products, users, unitOfWork, clock);
    }

    @Bean
    public StatsService statsService(ProductRepository products, OrderRepository orders, UserRepository users) {
        return new StatsService(products, orders, users);
    }
}
