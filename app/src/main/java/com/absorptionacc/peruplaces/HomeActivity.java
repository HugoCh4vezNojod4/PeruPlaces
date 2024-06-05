package com.absorptionacc.peruplaces;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    private static final String TAG = "HomeActivity";
    private FirebaseAuth mAuth;
    private GoogleMap mMap;
    private PlacesClient placesClient;
    private ArrayList<AutocompletePrediction> predictionList = new ArrayList<>();
    private ArrayAdapter<String> predictionsAdapter;
    private ArrayList<String> predictionsStrings = new ArrayList<>();
    private ListView lvPredictions;
    private Runnable runnable;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final LatLng LIMA_PERU = new LatLng(-12.046374, -77.0427934);
    private static final float DEFAULT_ZOOM = 10.0f;
    private boolean isSuggestionClicked = false;
    private FrameLayout slidingView;
    private BottomSheetBehavior<FrameLayout> bottomSheetBehavior;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        // Inicializa el SDK de Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtener el fragmento del mapa y registrar el callback de inicialización
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Error al cargar el mapa", Toast.LENGTH_SHORT).show();
        }

        predictionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, predictionsStrings);
        lvPredictions = findViewById(R.id.lvPredictions);
        lvPredictions.setAdapter(predictionsAdapter);

        // Configurar la vista deslizante
        slidingView = findViewById(R.id.sliding_view);
        bottomSheetBehavior = BottomSheetBehavior.from(slidingView);
        bottomSheetBehavior.setPeekHeight(150);  // Altura mínima visible cuando está colapsada
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // Estado inicial

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    slidingView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Manejar el evento de deslizamiento si es necesario
            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1.getY() - e2.getY() > 50 && Math.abs(velocityY) > 50) {
                    // Deslizar hacia arriba
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    return true;
                } else if (e2.getY() - e1.getY() > 50 && Math.abs(velocityY) > 50) {
                    // Deslizar hacia abajo
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    return true;
                }
                return false;
            }
        });

        // Agregar un touch listener a la vista de manejo para detectar el deslizamiento
        View handleView = findViewById(R.id.tvPlaceName); // Cambia esto al ID del handle
        handleView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void searchLocation(String query) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setCountries("PE")
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            predictionList.clear();
            predictionsStrings.clear();
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                predictionList.add(prediction);
                predictionsStrings.add(prediction.getPrimaryText(null).toString());
            }
            predictionsAdapter.notifyDataSetChanged();
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e(TAG, "Error al encontrar predicciones: " + apiException.getStatusCode());
            }
        });
    }

    private void fetchPlaceDetailsAndShowOnMap(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.RATING,
                Place.Field.LAT_LNG, Place.Field.TYPES, Place.Field.OPENING_HOURS, Place.Field.WEBSITE_URI,
                Place.Field.PHONE_NUMBER, Place.Field.PRICE_LEVEL, Place.Field.USER_RATINGS_TOTAL,
                Place.Field.BUSINESS_STATUS
        );

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);
        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            LatLng latLng = place.getLatLng();
            if (latLng != null) {
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }

            String phone = place.getPhoneNumber() == null ? "" : place.getPhoneNumber();
            String website = place.getWebsiteUri() == null ? "" : place.getWebsiteUri().toString();

            showPlaceDetails(
                    place.getName(),
                    place.getRating() != null ? String.format(Locale.US, "%.1f", place.getRating()) : "No rating",
                    place.getAddress() == null ? "" : place.getAddress(),
                    phone,
                    website
            );
        }).addOnFailureListener((exception) -> {
            Log.e(TAG, "Place not found: " + exception.getMessage());
        });
    }

    private void showPlaceDetails(String name, String rating, String address, String phone, String website) {
        TextView tvPlaceName = findViewById(R.id.tvPlaceName);
        TextView tvPlaceRating = findViewById(R.id.tvPlaceRating);
        TextView tvPlaceAddress = findViewById(R.id.tvPlaceAddress);
        TextView tvPlacePhone = findViewById(R.id.tvPlacePhone);
        TextView tvPlaceWebsite = findViewById(R.id.tvPlaceWebsite);

        // Configura el nombre del lugar
        if (name != null && !name.isEmpty()) {
            tvPlaceName.setText(name);
            tvPlaceName.setVisibility(View.VISIBLE);
        } else {
            tvPlaceName.setVisibility(View.GONE);
        }

        // Configura la calificación del lugar
        if (rating != null && !rating.equals("No rating")) {
            tvPlaceRating.setText(String.format("Rating: %s", rating));
            tvPlaceRating.setVisibility(View.VISIBLE);
        } else {
            tvPlaceRating.setVisibility(View.GONE);
        }

        // Configura la dirección del lugar
        if (address != null && !address.isEmpty()) {
            tvPlaceAddress.setText(address);
            tvPlaceAddress.setVisibility(View.VISIBLE);
        } else {
            tvPlaceAddress.setVisibility(View.GONE);
        }

        // Configura el teléfono y el manejador de clics
        if (phone != null && !phone.isEmpty()) {
            tvPlacePhone.setText(phone);
            tvPlacePhone.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            });
            tvPlacePhone.setVisibility(View.VISIBLE);
        } else {
            tvPlacePhone.setVisibility(View.GONE);
        }

        // Configura el sitio web y el manejador de clics
        if (website != null && !website.isEmpty()) {
            tvPlaceWebsite.setText(website);
            tvPlaceWebsite.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(website));
                startActivity(intent);
            });
            tvPlaceWebsite.setVisibility(View.VISIBLE);
        } else {
            tvPlaceWebsite.setVisibility(View.GONE);
        }

        slidingView.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true; // Permitir expandir la acción
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true; // Permitir colapsar la acción
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                handler.removeCallbacks(runnable);

                if (isSuggestionClicked) {
                    isSuggestionClicked = false;
                    return true;
                }
                runnable = () -> {
                    if (!newText.isEmpty()) {
                        searchLocation(newText);
                        lvPredictions.setVisibility(View.VISIBLE);
                    } else {
                        runOnUiThread(() -> {
                            predictionList.clear();
                            predictionsStrings.clear();
                            predictionsAdapter.notifyDataSetChanged();
                            lvPredictions.setVisibility(View.GONE);
                        });
                    }
                };

                handler.postDelayed(runnable, 300);
                return true;
            }
        });

        lvPredictions.setOnItemClickListener((parent, view, position, id) -> {
            isSuggestionClicked = true;
            String selectedText = predictionsStrings.get(position);
            searchView.setQuery(selectedText, false);  // Asegúrate de que searchView no sea null
            searchView.clearFocus();
            lvPredictions.setVisibility(View.GONE);

            AutocompletePrediction selectedPrediction = predictionList.get(position);
            fetchPlaceDetailsAndShowOnMap(selectedPrediction.getPlaceId());
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_more) {
            showPopupMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showPopupMenu() {
        View menuItemView = findViewById(R.id.action_more);
        PopupMenu popup = new PopupMenu(HomeActivity.this, menuItemView);
        popup.getMenuInflater().inflate(R.menu.user_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.menu_user_profile) {
                // Implementa la lógica para el perfil de usuario
                return true;
            } else if (menuItem.getItemId() == R.id.menu_sign_out) {
                signOut();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void signOut() {
        mAuth.signOut();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LIMA_PERU, DEFAULT_ZOOM));
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        // Manejar el evento de clic en el mapa
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        // Manejar el evento de clic prolongado en el mapa
    }

    private String formatOpeningHours(String jsonHours) {
        // Pseudocódigo: parsea el JSON y conviértelo a un formato más legible
        return "Formatted Hours"; // Retorna una cadena legible
    }
}
