package com.projects.safariapps.flagquiz;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import android.os.Handler;


/**
 * A placeholder fragment containing a simple view.
 */

public class MainActivityFragment extends Fragment {

    private static final String TAG = "FlagQuiz Activity";
    private static final int FLAGS_IN_QUIZ = 10; // Total number of flags participate in quiz

    private List<String> fileNameList; // Flag names in a region
    private List<String> quizCountriesList; // Countries List participating in the quiz
    private Set regionSet; // World regions enabled in current quiz

    private int guessRows; // No of GuessRows to be displayed
    private int correctAnswers; // Number of correct answers
    private int totalGuesses; // Number of total guesses

    private SecureRandom random;

    private Handler handler; // load flag after a delay

    private String correctAnswer;// correct country for the current flag

    private LinearLayout quizLinearLayout; // Layout that contains quiz
    private TextView questionNumberTextView; // To show current question number
    private ImageView flagImageView; // To display a country flag
    private LinearLayout[] guessLinearLayouts;
    private TextView answerTextView;


    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_main, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random =new SecureRandom();
        handler = new Handler();

        // Get references to GUI components
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);

        // configure Listeners for guess Buttons

        for (LinearLayout row : guessLinearLayouts){

            for (int column = 0; column < row.getChildCount(); column++){

                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        // set questionNumbersTextView
        questionNumberTextView.setText(getString(R.string.question,1,FLAGS_IN_QUIZ));

        return view;
    }


    // Update Guess Ros based on value in Shared preferences
    public void updateGuessRows(SharedPreferences sharedPreferences){
        //Get the number of Guess Buttons that to be displayed
        String choices = sharedPreferences.getString(MainActivity.CHOICES,null);
        guessRows = Integer.parseInt(choices)/2;

        // Hide all the question button Linear Layouts
        for (LinearLayout linearLayout : guessLinearLayouts)
            linearLayout.setVisibility(View.GONE);

        // Display appropriate Guess Button Linear Layouts
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }

    // Update World Regions based on values in shared preferences
    public void updateRegions(SharedPreferences sharedPreferences){
        regionSet = sharedPreferences.getStringSet(MainActivity.REGIONS,null);
    }

    // Setup and start the quiz
    public void resetQuiz() {

        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();

        try {
            for (Object region : regionSet){

                String[] paths = assets.list((String) region);
                // Loop through region set to extract each country flag names

                for (String path : paths){
                    fileNameList.add(path.replace(".png",""));
                }
            }
        } catch (IOException e)
        {
            Log.e(TAG,"Error in loading regions",e);
        }


        correctAnswers = 0;
        totalGuesses = 0;

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        // Add random file names to the quizCountriesList

        while (flagCounter <= FLAGS_IN_QUIZ){

            int randomIndex = random.nextInt(numberOfFlags);

            // Get the random filename
            String fileName = fileNameList.get(randomIndex);

            // If the region is enabled and it hasn't already been chosen
            if (!quizCountriesList.contains(fileName)){
                quizCountriesList.add(fileName);
                flagCounter++;
            }
        }

        loadNextFlag();
    }

    // after user guesses a correct flag, load the next flag
    private void loadNextFlag(){

        // get file name of the next flag and remove it from the list

        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;
        answerTextView.setText("");

        // Display correct question number

        questionNumberTextView.setText(getString(R.string.question,(correctAnswers+1),FLAGS_IN_QUIZ));

        // Extract the region from next image's name
        String region = nextImage.substring(0,nextImage.indexOf('-'));

        // Use Asset Manager to load next image from Assets folder

        AssetManager assets = getActivity().getAssets();

        // Get an Input Stream to the Asset representing the next flag and try to use the InputStream
        try{
            InputStream stream = assets.open(region + "/" + nextImage + ".png");
            // load the asset as drawable and display on flagImageView
            Drawable flag = Drawable.createFromStream(stream,nextImage);
            flagImageView.setImageDrawable(flag);
        }
        catch (IOException e){
            Log.e(TAG,"Error in Loading"+nextImage,e);
        }

        Collections.shuffle(fileNameList); // Shuffle file names

        // put the correct answer at the end of list

        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));


        // Add 2,4,6 or 8 guess buttons based on the value of guess rows

        for (int row = 0; row < guessRows; row++){
            // place buttons in the current table row

            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++){
                // Get the reference to Button and configure

                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                // Get Country Name and set it as new GuessButton's text

                String fileName = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(fileName));
            }
        }

        // randomly replace one button with the correct answer

        int row = random.nextInt(guessRows); // pick random row
        int column = random.nextInt(2); // pick random column
        LinearLayout randomRow = guessLinearLayouts[row];
        String countryName = getCountryName(correctAnswer);
        ((Button)randomRow.getChildAt(column)).setText(countryName);

    }

    // parses the country flag name and returns the country name
    private String getCountryName(String name) {

        return name.substring(name.indexOf('-')+1).replace('_',' ');
    }

    // called when a guess button is touched
    private View.OnClickListener  guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Button guessButton = (Button) view;
            String answer = getCountryName(correctAnswer);
            String guess = guessButton.getText().toString();
            ++totalGuesses;

            // Answer is correct, then Increment the number of correct answers
            if (guess.equals(answer)) {
                ++correctAnswers;


                // Display the correct answer in Green Text
                answerTextView.setText(answer + " !");
                answerTextView.setTextColor(getResources().getColor(R.color.correct, getContext().getTheme()));

                // Disable all guess buttons
                disableButtons();

                // If the user has correctly identified FLAGS_IN_QUIZ flags
                if (correctAnswers == FLAGS_IN_QUIZ) {
                    // DialogFragment to display quiz stats and start new quiz

                    // create an Alert Dialog and return it

                    DialogFragment quizResults = new DialogFragment() {

                        @Override
                        public Dialog onCreateDialog(Bundle bundle) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(getString(R.string.results, totalGuesses, (1000 / (double) totalGuesses)));

                            // ResetQuiz button
                            builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    resetQuiz();
                                }
                            });

                            return builder.create();
                        }
                    };

                    // use Fragment Manager to display Fragment
                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(),"Quiz Results");
                }
                else {
                    // Answer is correct but quiz is not over. Load the next flag after a second delay -netspeed full -avd Nexus_6_API_23
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loadNextFlag();
                        }
                    }, 2000);
                }
            }
            else {
                // If the answer is incorrect, display Incorrect! in red

                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect,getContext().getTheme()));
                guessButton.setEnabled(false);

            }
        }
    };

    private void disableButtons(){

        for (int row = 0; row < guessRows; row++){

            LinearLayout guessRow = guessLinearLayouts[row];

            for(int column = 0; column < guessRow.getChildCount(); column++){
                guessRow.getChildAt(column).setEnabled(false);
            }
        }
    }
}
