package com.dearlavion.storeengine.catalog;

import com.dearlavion.storeengine.product.ProductMapper;
import com.dearlavion.storeengine.product.ProductRepository;
import com.dearlavion.storeengine.product.ProductService;
import com.dearlavion.storeengine.product.model.Product;
import com.dearlavion.storeengine.product.request.CreateProductRequest;
import com.dearlavion.storeengine.product.request.UpdateProductRequest;
import com.dearlavion.storeengine.productitem.ProductItemMapper;
import com.dearlavion.storeengine.productitem.ProductItemRepository;
import com.dearlavion.storeengine.productitem.ProductItemService;
import com.dearlavion.storeengine.productitem.model.ProductItem;
import com.dearlavion.storeengine.productitem.request.CreateProductItemRequest;
import com.dearlavion.storeengine.productitem.request.UpdateProductItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Every write path that changes what a survey should see must refresh the cached catalog. A missed
 * hook here is invisible at runtime — the admin saves, gets a success, and surveys keep serving the
 * old catalog with no error anywhere — so each of the seven is pinned individually.
 */
class CacheInvalidationTest {

    /** ProductItemService.getById rejects anything that isn't a valid ObjectId before it reaches
     *  the repository, so the fixture needs a real one rather than a readable placeholder. */
    private static final String ITEM_ID = "507f1f77bcf86cd799439011";

    private CatalogCache catalog;
    private ProductRepository products;
    private ProductItemRepository items;
    private ProductService productService;
    private ProductItemService itemService;

    @BeforeEach
    void setUp() {
        catalog = Mockito.mock(CatalogCache.class);
        products = Mockito.mock(ProductRepository.class);
        items = Mockito.mock(ProductItemRepository.class);
        ProductMapper productMapper = Mockito.mock(ProductMapper.class);
        ProductItemMapper itemMapper = Mockito.mock(ProductItemMapper.class);
        MongoTemplate mongo = Mockito.mock(MongoTemplate.class);

        Product product = new Product();
        product.setId("p1");
        Mockito.when(products.findById(Mockito.anyString())).thenReturn(Optional.of(product));
        Mockito.when(products.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(productMapper.toEntity(Mockito.any(), Mockito.any())).thenReturn(product);
        Mockito.when(products.existsById(Mockito.anyString())).thenReturn(false);
        Mockito.when(mongo.exists(Mockito.any(), Mockito.eq(Product.class))).thenReturn(false);

        ProductItem item = new ProductItem();
        item.setId(ITEM_ID);
        item.setProductId("p1");
        Mockito.when(items.findById(Mockito.anyString())).thenReturn(Optional.of(item));
        Mockito.when(items.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(itemMapper.toEntity(Mockito.any())).thenReturn(item);
        Mockito.when(items.findByProductIdAndActiveTrue(Mockito.anyString(), Mockito.any(Sort.class)))
                .thenReturn(List.of(item));

        itemService = new ProductItemService(items, mongo, itemMapper, catalog);

        @SuppressWarnings("unchecked")
        ObjectProvider<ProductItemService> provider = Mockito.mock(ObjectProvider.class);
        Mockito.when(provider.getObject()).thenReturn(itemService);
        productService = new ProductService(products, mongo, productMapper, provider, catalog);
    }

    private void assertRefreshed() {
        Mockito.verify(catalog, Mockito.atLeastOnce()).refresh();
    }

    @Test
    void productCreateRefreshes() {
        CreateProductRequest dto = Mockito.mock(CreateProductRequest.class);
        Mockito.when(dto.name()).thenReturn("Test Product"); // create() slugifies this into the id
        productService.create(dto);
        assertRefreshed();
    }

    @Test
    void productUpdateRefreshes() {
        productService.update("p1", Mockito.mock(UpdateProductRequest.class));
        assertRefreshed();
    }

    @Test
    void productDeactivateRefreshes() {
        productService.deactivate("p1");
        assertRefreshed();
    }

    @Test
    void itemCreateRefreshes() {
        itemService.create(Mockito.mock(CreateProductItemRequest.class));
        assertRefreshed();
    }

    @Test
    void itemUpdateRefreshes() {
        itemService.update(ITEM_ID, Mockito.mock(UpdateProductItemRequest.class));
        assertRefreshed();
    }

    @Test
    void itemDeactivateRefreshes() {
        itemService.deactivate(ITEM_ID);
        assertRefreshed();
    }

    @Test
    void deactivateForProductRefreshes() {
        itemService.deactivateForProduct("p1");
        assertRefreshed();
    }
}
