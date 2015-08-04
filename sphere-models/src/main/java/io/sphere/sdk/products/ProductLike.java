package io.sphere.sdk.products;

import io.sphere.sdk.models.ResourceView;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.taxcategories.TaxCategory;

import javax.annotation.Nullable;

interface ProductLike<T> extends ResourceView<T>, ProductIdentifiable {
    Reference<ProductType> getProductType();

    @Nullable
    Reference<TaxCategory> getTaxCategory();
}
