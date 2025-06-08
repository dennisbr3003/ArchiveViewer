package com.dennisbrink.mt.global.mypackedfileviewer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class ImageLoginActivity extends AppCompatActivity implements IZipApplication {

    private boolean setCoordinates = false;
    private Coordinates coordinates = new Coordinates();
    private Coordinates coordinatesUser = new Coordinates();

    private ImageView imageTouchable;
    private TextView textViewCoordinates;
    private ConstraintLayout rootLayout;
    private int touchCount = 0;

    @SuppressLint({"ClickableViewAccessibility", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_login);

        textViewCoordinates = findViewById(R.id.textViewCoordinates);

        int originalColor = textViewCoordinates.getCurrentTextColor();


        imageTouchable = findViewById(R.id.imageTouchable);
        rootLayout = findViewById(R.id.main);
        // check if there are any coordinates, if there are load them into a Object of type Coordinates
        String coordinatesAsString = ZipUtilities.loadDataFromFile(IZipApplication.COORDINATE_FILENAME, IZipApplication.COORDINATE_DIR);
        // coordinates = ZipUtilities.jsonToCoordinates(coordinatesAsString); // string is never empty, there is always some initial data as the deviation

        Log.d("DB1", "Coordinates found ? " + (!coordinatesAsString.isEmpty()));
        if(coordinatesAsString.isEmpty()){
            setCoordinates = true;
            textViewCoordinates.setTextColor(originalColor);
            textViewCoordinates.setText(String.format("%s%d", getString(R.string.nr_of_coordinates), coordinates.getCoordinates().size()));
        } else {
            Log.d("DB1", "coordinates: " + coordinatesAsString);
            // load this string into it's object structure
            coordinates = ZipUtilities.jsonToCoordinates(coordinatesAsString);
            if (coordinates.getCoordinates().isEmpty()) {
                setCoordinates = true;
                textViewCoordinates.setTextColor(originalColor);
                textViewCoordinates.setText(String.format("%s%d", getString(R.string.nr_of_coordinates), coordinates.getCoordinates().size()));
            } else {
                textViewCoordinates.setText("");
            }
        }

        imageTouchable.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                Log.d("DB1", "Coordinates x: " + x + " y: " + y);
                Coordinate coordinate = new Coordinate((int) x, (int) y);
                try {
                    if(setCoordinates) {
                        coordinates.getCoordinates().add(coordinate);
                        textViewCoordinates.setTextColor(originalColor);
                        textViewCoordinates.setText(String.format("%s%d", getString(R.string.nr_of_coordinates), coordinates.getCoordinates().size()));
                    } else {
                        coordinatesUser.getCoordinates().add(coordinate);
                        textViewCoordinates.setTextColor(originalColor);
                        textViewCoordinates.setText(String.format("%s%d", "Number of user coordinates: ", coordinatesUser.getCoordinates().size()));

                    }
                } catch(Exception e) {
                    Log.d("DB1", "coordinate not added: " + e.getMessage());
                }
                try {
                    addTouchMarker(x, y);
                } catch(Exception e){
                    Log.d("DB1", "Error placing marker : " + e.getMessage());
                }
            }
            return true; // Return true to indicate the event was handled
        });

        ImageButton buttonGo = findViewById(R.id.imageButtonGo);
        buttonGo.setOnClickListener(view -> {
            // check if the checkpoints are complete
            Log.d("DB1", "user entered touch points ? " +  !coordinatesUser.getCoordinates().isEmpty());
            Log.d("DB1", "Are there saved touch points ? " +  !coordinates.getCoordinates().isEmpty());

            if(coordinatesUser.getCoordinates().isEmpty()){
                // not correct, you cannot have 0 touch points
                textViewCoordinates.setTextColor(getResources().getColor(R.color.red, null));
                textViewCoordinates.setText(R.string.at_least_one_touch_point_is_required);
                return;
            }
            if(coordinatesUser.getCoordinates().size() != coordinates.getCoordinates().size()){
                textViewCoordinates.setTextColor(getResources().getColor(R.color.red, null));
                textViewCoordinates.setText(R.string.touchpoints_do_not_match);
                // clear markers, and the coordinates the user entered
                coordinatesUser.setCoordinates(new ArrayList<>());
                removeAllMarkers();
                return;
            }

            // check the proximity and order of the entered touch points using the deviation,
            // I need the index, hence a traditional loop
            for (int i = 0; i < coordinatesUser.getCoordinates().size(); i++) {
                // You have access to the index 'i' here
                // Do something with the index or the coordinate
                Coordinate coordinate = coordinatesUser.getCoordinates().get(i);
                if ((coordinate.getX() >= (coordinates.getCoordinates().get(i).getX()) - coordinates.getDeviation() &&
                     coordinate.getX() <= (coordinates.getCoordinates().get(i).getX()) + coordinates.getDeviation()) &&
                    (coordinate.getY() >= (coordinates.getCoordinates().get(i).getY()) - coordinates.getDeviation() &&
                     coordinate.getY() <= (coordinates.getCoordinates().get(i).getY()) + coordinates.getDeviation())) {
                    Log.d("DB1", "Coordinate X user " + coordinate.getX());
                    Log.d("DB1", " is in between " + (coordinates.getCoordinates().get(i).getX() - coordinates.getDeviation()));
                    Log.d("DB1", " and " + (coordinates.getCoordinates().get(i).getX() + coordinates.getDeviation()));
                    Log.d("DB1", "Coordinate Y user " + coordinate.getY());
                    Log.d("DB1", " is in between " + (coordinates.getCoordinates().get(i).getY() - coordinates.getDeviation()));
                    Log.d("DB1", " and " + (coordinates.getCoordinates().get(i).getY() + coordinates.getDeviation()));
                }
                else {
                    // not good we are not close enough
                    textViewCoordinates.setTextColor(getResources().getColor(R.color.red, null));
                    textViewCoordinates.setText(R.string.touchpoints_do_not_match);
                    // clear markers, and the coordinates the user entered
                    coordinatesUser.setCoordinates(new ArrayList<>());
                    removeAllMarkers();
                    return;
                }
            }

            Intent intent = new Intent(ImageLoginActivity.this, ZipLibraryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        ImageButton buttonUndo = findViewById(R.id.imageButtonUndo);
        buttonUndo.setOnClickListener(view -> {
            // clear coordinates
            coordinates.setCoordinates(new ArrayList<>());
            removeAllMarkers();
            textViewCoordinates.setTextColor(originalColor);
            textViewCoordinates.setText(String.format("%s%d", getString(R.string.nr_of_coordinates), coordinates.getCoordinates().size()));
        });

        ImageButton buttonCheckOk = findViewById(R.id.imageButtonCheckOk);
        buttonCheckOk.setOnClickListener(view -> {

            Log.d("DB1", "user entered touch points ? " +  coordinatesUser.getCoordinates().isEmpty());
            Log.d("DB1", "Are there saved touch points ? " +  coordinatesUser.getCoordinates().isEmpty());

            if(coordinates.getCoordinates().isEmpty()){
                // not correct, you cannot have 0 touch points
                textViewCoordinates.setTextColor(getResources().getColor(R.color.red, null));
                textViewCoordinates.setText(R.string.at_least_one_touch_point_is_required);
                return;
            }

            // done entering touch points, reload and ask touch point coordinates
            ZipUtilities.saveDataToFile(COORDINATE_FILENAME, COORDINATE_DIR, ZipUtilities.coordinatesToJson(coordinates));
            buttonUndo.setVisibility(View.GONE);
            buttonCheckOk.setVisibility(View.GONE);
            buttonGo.setVisibility(View.VISIBLE);
            // reload everything her because we need to verify if coordinates match
            // textViewCoordinates.setVisibility(View.INVISIBLE);
            removeAllMarkers();
            setCoordinates = false; // not setting anymore, points are saved
            textViewCoordinates.setText("");
        });

        if(setCoordinates) {
            buttonGo.setVisibility(View.GONE);
        } else {
            buttonUndo.setVisibility(View.GONE);
            buttonCheckOk.setVisibility(View.GONE);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void addTouchMarker(float x, float y) {
        touchCount++;

        View markerView = LayoutInflater.from(this).inflate(R.layout.marker_layout, rootLayout, false);
        TextView markerNumber = markerView.findViewById(R.id.marker_number);
        markerNumber.setText(String.valueOf(touchCount));

        // Set a unique ID for the marker view
        markerView.setId(View.generateViewId());
        markerView.setTag("marker");

        rootLayout.addView(markerView);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(rootLayout);

        // Convert touch position to root layout position
        int[] imageViewLocation = new int[2];
        imageTouchable.getLocationOnScreen(imageViewLocation);

        int adjustedX = (int) (imageViewLocation[0] + x);
        int adjustedY = (int) (imageViewLocation[1] + y);

        // Measure and adjust for centering
        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int markerWidth = markerView.getMeasuredWidth();
        int markerHeight = markerView.getMeasuredHeight();

        adjustedX -= markerWidth / 2;
        adjustedY -= markerHeight / 2;

        // Adjust upwards by a few pixels (3dp converted to pixels)
        float density = getResources().getDisplayMetrics().density;
        adjustedY -= (int) (20 * density);

        // Apply constraints for correct positioning
        constraintSet.connect(markerView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, adjustedX);
        constraintSet.connect(markerView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, adjustedY);

        constraintSet.applyTo(rootLayout);
    }

    private void removeAllMarkers() {
        for (int i = rootLayout.getChildCount() - 1; i >= 0; i--) {
            View child = rootLayout.getChildAt(i);
            if ("marker".equals(child.getTag())) {
                rootLayout.removeView(child);
            }
        }
        touchCount = 0;
    }

}