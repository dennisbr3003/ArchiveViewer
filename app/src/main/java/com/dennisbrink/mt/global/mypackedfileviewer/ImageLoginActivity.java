package com.dennisbrink.mt.global.mypackedfileviewer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
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

import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.Coordinate;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.Coordinates;

import java.util.ArrayList;

public class ImageLoginActivity extends AppCompatActivity implements IZipApplication {

    private boolean setCoordinates = false;
    private boolean verifyCoordinateSave = false;
    private Coordinates coordinates = new Coordinates();
    private final Coordinates coordinatesUser = new Coordinates();

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
        imageTouchable = findViewById(R.id.imageTouchable);
        rootLayout = findViewById(R.id.main);

        int originalColor = textViewCoordinates.getCurrentTextColor();

        // check if there are any coordinates, if there are load them into a Object of type Coordinates
        String coordinatesAsString = ZipUtilities.loadDataFromFile(IZipApplication.COORDINATE_FILENAME, IZipApplication.COORDINATE_DIR);
        textViewCoordinates.setTextColor(originalColor);
        textViewCoordinates.setText("");

        if(coordinatesAsString.isEmpty()){ // this is almost never only at startup and after deletion
            setCoordinates = true; // we are setting a touch point sequence
        } else {
            coordinates = ZipUtilities.jsonToCoordinates(coordinatesAsString); // load this string into it's object structure
            if (coordinates.getCoordinates().isEmpty()) {
                setCoordinates = true; // we are setting a touch point sequence
            }
        }

        // image touch listener
        imageTouchable.setOnTouchListener((view, motionEvent) -> {

            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                textViewCoordinates.setTextColor(originalColor);
                if (setCoordinates) {
                    textViewCoordinates.setText(R.string.new_personal_sequence);
                } else { // regular entry of touch points
                    if(verifyCoordinateSave) { // verification after new sequence
                        textViewCoordinates.setText(R.string.verify_personal_sequence);
                    } else {
                        textViewCoordinates.setText(R.string.personal_sequence_go);
                    }
                }
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                Coordinate coordinate = new Coordinate((int) x, (int) y);

                textViewCoordinates.setTextColor(originalColor);

                if(setCoordinates) coordinates.getCoordinates().add(coordinate);
                else coordinatesUser.getCoordinates().add(coordinate);

                addTouchMarker(x, y);
            }
            return true; // Return true to indicate the event was handled

        });

        ImageButton buttonGo = findViewById(R.id.imageButtonGo);
        buttonGo.setOnClickListener(view -> {

            // check if the checkpoints are complete
            if(coordinatesUser.getCoordinates().isEmpty()){ // not correct, you cannot have 0 touch points
                textViewCoordinates.setTextColor(getResources().getColor(R.color.red, null));
                textViewCoordinates.setText(R.string.at_least_one_touch_point_is_required);
                return;
            }

            // check if the sizes match, a first simple check
            if(coordinatesUser.getCoordinates().size() != coordinates.getCoordinates().size()){
                textViewCoordinates.setTextColor(getResources().getColor(R.color.red, null));
                textViewCoordinates.setText(R.string.touchpoints_do_not_match);
                coordinatesUser.setCoordinates(new ArrayList<>()); // clear markers, and the coordinates the user entered
                removeAllMarkers();
                return;
            }

            // check the proximity and order of the entered touch points using the deviation, I need the index, hence a traditional loop
            for (int i = 0; i < coordinatesUser.getCoordinates().size(); i++) {
                Coordinate coordinate = coordinatesUser.getCoordinates().get(i);
                if  (!((coordinate.getX() >= (coordinates.getCoordinates().get(i).getX()) - coordinates.getDeviation() &&
                        coordinate.getX() <= (coordinates.getCoordinates().get(i).getX()) + coordinates.getDeviation()) &&
                       (coordinate.getY() >= (coordinates.getCoordinates().get(i).getY()) - coordinates.getDeviation() &&
                        coordinate.getY() <= (coordinates.getCoordinates().get(i).getY()) + coordinates.getDeviation()))) {
                    textViewCoordinates.setTextColor(getResources().getColor(R.color.red, null));
                    textViewCoordinates.setText(R.string.touchpoints_do_not_match);
                    coordinatesUser.setCoordinates(new ArrayList<>()); // clear markers, and the coordinates the user entered, the user must restart without any clue
                    removeAllMarkers();
                    return;
                }
            }

            Intent intent = new Intent(ImageLoginActivity.this, ZipLibraryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // do not use this activity in the stack
            startActivity(intent);
        });

        ImageButton buttonUndo = findViewById(R.id.imageButtonUndo);
        buttonUndo.setOnClickListener(view -> {
            coordinates.setCoordinates(new ArrayList<>()); // clear coordinates
            removeAllMarkers(); // clear markers. Any text remains the same
        });

        ImageButton buttonCheckOk = findViewById(R.id.imageButtonCheckOk);
        buttonCheckOk.setOnClickListener(view -> {

            if(coordinates.getCoordinates().isEmpty()){  // not correct, you cannot have 0 touch points
                textViewCoordinates.setTextColor(getResources().getColor(R.color.red, null));
                textViewCoordinates.setText(R.string.at_least_one_touch_point_is_required);
                return;
            }

            // done entering touch points, reload and ask touch point coordinates
            ZipUtilities.saveDataToFile(COORDINATE_FILENAME, COORDINATE_DIR, ZipUtilities.coordinatesToJson(coordinates));
            buttonUndo.setVisibility(View.GONE);
            buttonCheckOk.setVisibility(View.GONE);
            buttonGo.setVisibility(View.VISIBLE);

            // reload everything here because we need to verify if coordinates match
            removeAllMarkers();
            setCoordinates = false; // not setting anymore, points are saved
            verifyCoordinateSave = true;
            textViewCoordinates.setText(R.string.verify_personal_sequence);

        });

        // init buttons on load of this activity, depending on if coordinates being set or entered
        if(setCoordinates) {
            buttonGo.setVisibility(View.GONE);
            textViewCoordinates.setText(R.string.new_personal_sequence);
        } else {
            textViewCoordinates.setText(R.string.personal_sequence_go);
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
        markerView.setTag(getString(R.string.marker));

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
            if (ZipApplication.getAppContext().getResources().getString(R.string.marker).equals(child.getTag())) {
                rootLayout.removeView(child);
            }
        }
        touchCount = 0;
    }

}