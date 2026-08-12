package com.anokix.distributorapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.data.ApiMapper;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.BaseInfoData;
import com.anokix.distributorapp.network.dto.ProductsData;
import com.anokix.distributorapp.ui.AddProductOptionsActivity;
import com.anokix.distributorapp.ui.CreateProductActivity;
import com.anokix.distributorapp.ui.NotificationBadge;
import com.anokix.distributorapp.ui.NotificationsActivity;
import com.anokix.distributorapp.ui.adapter.ProductAdapter;
import com.anokix.distributorapp.ui.util.ProductListSection;

public class ProductsFragment extends Fragment {

    private ProductAdapter adapter;
    private ProductListSection section;

    private final ActivityResultLauncher<Intent> wizardLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    loadProducts();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            NotificationBadge.refresh(requireContext(), getView().findViewById(R.id.notificationBadge));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.notificationsButton).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationsActivity.class)));

        view.findViewById(R.id.addProductFab).setOnClickListener(v ->
                wizardLauncher.launch(new Intent(requireContext(), AddProductOptionsActivity.class)));

        RecyclerView list = view.findViewById(R.id.productsList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProductAdapter();
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(requireContext(), CreateProductActivity.class);
            intent.putExtra(CreateProductActivity.EXTRA_PRODUCT_ID, item.id);
            wizardLauncher.launch(intent);
        });
        list.setAdapter(adapter);

        section = new ProductListSection(requireContext(), adapter::setItems);
        section.bind(view);

        loadProducts();
        loadFilterOptions();
    }

    private void loadProducts() {
        ApiClient.get(requireContext()).getProducts(new ApiCallback<ProductsData>() {
            @Override
            public void onSuccess(ProductsData data) {
                if (isAdded()) {
                    section.setProducts(ApiMapper.toProductItems(data),
                            data == null ? null : data.summary);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /** Categories + brands for the filter pickers; failures leave the product-derived lists. */
    private void loadFilterOptions() {
        ApiClient.get(requireContext()).getBaseInfo(new ApiCallback<BaseInfoData>() {
            @Override
            public void onSuccess(BaseInfoData data) {
                if (isAdded()) {
                    section.setBaseInfo(data);
                }
            }

            @Override
            public void onError(String message) {
                // Non-fatal: the pickers fall back to values found on the loaded products.
            }
        });
    }
}
