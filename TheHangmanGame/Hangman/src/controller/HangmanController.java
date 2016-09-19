package controller;

import apptemplate.AppTemplate;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.internal.fastinfoset.util.CharArray;
import data.GameData;
import data.GameDataFile;
import data.guessList;
import gui.Workspace;
import javafx.animation.AnimationTimer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import propertymanager.PropertyManager;
import ui.AppMessageDialogSingleton;
import ui.YesNoCancelDialogSingleton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;


import static settings.AppPropertyType.*;

/**
 * @author Ritwik Banerjee
 */
public class HangmanController implements FileController {

    private AppTemplate appTemplate; // shared reference to the application
    private GameData    gamedata;    // shared reference to the game being played, loaded or saved
    private Text[]      progress;    // reference to the text area for the word
    private boolean     success;     // whether or not player was successful
    private int         discovered;  // the number of letters already discovered
    private Button      gameButton;  // shared reference to the "start game" button
    private Label       remains;     // dynamically updated label that indicates the number of remaining guesses
    private boolean     gameover;    // whether or not the current game is already over
    private boolean     savable;
    private File workFile;

    public GameData newGameData;
    public ObjectMapper mapper;
    public guessList newGuessList;
    public guessList GuessList;
    public String target;
    public String guessWord;
    public Set<Character> forCorrectGuess = new HashSet<Character>();
    public Path workfile;


    public HangmanController(AppTemplate appTemplate, Button gameButton) {
        this(appTemplate);
        this.gameButton = gameButton;
        newGameData = new GameData(appTemplate);

    }

    public HangmanController(AppTemplate appTemplate) {
        this.appTemplate = appTemplate;
    }


    public void enableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
        }
        gameButton.setDisable(false);
    }


    public void start() {

        gamedata = new GameData(appTemplate);
        newGameData= gamedata;


        //newGameData.
        GuessList = new guessList();
        newGuessList= GuessList;
        System.out.println(gamedata.getTargetWord());
        String wordTarget=new String(newGameData.getTargetWord());
        //System.out.println(wordTarget);
/**
        try {
            GuessList.setJSONTargetWord(workFile, wordTarget);
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        gameover = false;
        success = false;
        savable = true;
        discovered = 0;
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        appTemplate.getGUI().updateWorkspaceToolbar(savable);
        HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
        HBox guessedLetters    = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);

        remains = new Label(Integer.toString(GameData.TOTAL_NUMBER_OF_GUESSES_ALLOWED));
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);
        initWordGraphics(guessedLetters);

        play();
    }

    private void end() {
        System.out.println(success ? "You win!" : "Ah, close but not quite there. The word was \"" + gamedata.getTargetWord() + "\".");
        appTemplate.getGUI().getPrimaryScene().setOnKeyTyped(null);
        gameover = true;
        gameButton.setDisable(true);
        savable = false; // cannot save a game that is already over
        appTemplate.getGUI().updateWorkspaceToolbar(savable);
    }

    public String findingTarget(){
        target = gamedata.getTargetWord();
        char[] targetReturn = new char[target.length()];
        for(int i = 0 ; i<target.length();i++){
            targetReturn[i] = target.charAt(i);
        }
        return  targetReturn.toString();
    }



    private void initWordGraphics(HBox guessedLetters) {
        char[] targetword = gamedata.getTargetWord().toCharArray();

        progress = new Text[targetword.length];
        for (int i = 0; i < progress.length; i++) {
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(false);
        }
        guessedLetters.getChildren().addAll(progress);
    }

    public void play() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {

                guessWord = findingTarget();
                GuessList.setTargetWord(guessWord);



                appTemplate.getGUI().getPrimaryScene().setOnKeyTyped((KeyEvent event) -> {
                    char guess = event.getCharacter().charAt(0);
                    if (!alreadyGuessed(guess)) {
                        boolean goodguess = false;
                        for (int i = 0; i < progress.length; i++) {
                            if (gamedata.getTargetWord().charAt(i) == guess) {
                                gamedata.addGoodGuess(guess);
                                forCorrectGuess.add(guess);
                                newGameData.setGoodGuesses(forCorrectGuess);
                                GuessList.setCorrectGuess(gamedata.getGoodGuesses());
                                progress[i].setVisible(true);
                                goodguess = true;
                                discovered++;
                            }
                        }
                        if (!goodguess){
                            gamedata.addBadGuess(guess);
                            GuessList.setWrongGuess(gamedata.getBadGuesses());

                        }
                        success = (discovered == progress.length);
                        remains.setText(Integer.toString(gamedata.getRemainingGuesses()));
                    }
                });
                if (gamedata.getRemainingGuesses() <= 0 || success)
                    stop();
            }

            @Override
            public void stop() {
                super.stop();
                end();
            }
        };
        timer.start();
    }



    private boolean alreadyGuessed(char c) {
        return gamedata.getGoodGuesses().contains(c) || gamedata.getBadGuesses().contains(c);
    }

    @Override
    public void handleNewRequest() {
        AppMessageDialogSingleton messageDialog   = AppMessageDialogSingleton.getSingleton();
        PropertyManager           propertyManager = PropertyManager.getManager();
        boolean                   makenew         = true;
        if (savable)
            try {
                makenew = promptToSave();
            } catch (IOException e) {
                messageDialog.show(propertyManager.getPropertyValue(NEW_ERROR_TITLE), propertyManager.getPropertyValue(NEW_ERROR_MESSAGE));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        if (makenew) {
            appTemplate.getDataComponent().reset();                // reset the data (should be reflected in GUI)
            appTemplate.getWorkspaceComponent().reloadWorkspace(); // load data into workspace
            ensureActivatedWorkspace();                            // ensure workspace is activated
            workFile = null;                                       // new workspace has never been saved to a file

            Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameWorkspace.reinitialize();
            enableGameButton();
        }

        if (gameover) {
            savable = false;
            appTemplate.getGUI().updateWorkspaceToolbar(savable);
            Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameWorkspace.reinitialize();
            enableGameButton();
        }

    }


    /**
     * Handling the new Request from the user to get the button working
     *
     * @author Biki and Ritwik
     * @throws IOException
     */

    @Override
    public void handleSaveRequest() throws IOException {

        //System.out.println(newGameData.getGoodGuesses());

        GameDataFile savedFile = new GameDataFile();
       // workFile = Paths.get("");

        savedFile.saveData(gamedata , );

        boolean saveIns = false;
        try {
            saveIns = promptToSave();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if(!saveIns){
            try {

            }
            catch (NullPointerException e) {
                e.printStackTrace();
                AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
                PropertyManager           props  = PropertyManager.getManager();
                dialog.show(props.getPropertyValue(SAVE_ERROR_TITLE), props.getPropertyValue(SAVE_ERROR_MESSAGE));
            }
        }
    }

    /**
     * A helper method to save work. It saves the work, marks the current work file as saved, notifies the user, and
     * updates the appropriate controls in the user interface
     *
     * @param selectedFile The file to which the work will be saved.
     * @throws IOException
     */

    private void saveWork(File selectedFile) throws IOException {
        try{
            Path filePath= Paths.get(selectedFile.getAbsolutePath());
            save(filePath);
            savable = true;
        }
        catch (IOException e) {
            e.printStackTrace();
            AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
            PropertyManager           props  = PropertyManager.getManager();
            dialog.show(props.getPropertyValue(SAVE_ERROR_TITLE), props.getPropertyValue(SAVE_ERROR_MESSAGE));
        }
    }

    /**
     * A helper method to save work. It saves the work, marks the current work file as saved, notifies the user, and
     * updates the appropriate controls in the user interface
     *
     * @param target The file to which the work will be saved.
     * @throws IOException
     */
    private void save(Path target) throws IOException {

        mapper = new ObjectMapper();
        File jsonWriteFile= new File(String.valueOf(target));

        mapper.writeValue(jsonWriteFile,newGuessList);
    }

    @Override
    public void handleLoadRequest() throws IOException{


    }

    @Override
    public void handleExitRequest() {
        try {
            boolean exit = true;
            if (savable)
                exit = promptToSave();
            if (exit)
                System.exit(0);
        } catch (IOException ioe) {
            AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
            PropertyManager           props  = PropertyManager.getManager();
            dialog.show(props.getPropertyValue(SAVE_ERROR_TITLE), props.getPropertyValue(SAVE_ERROR_MESSAGE));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void ensureActivatedWorkspace() {
        appTemplate.getWorkspaceComponent().activateWorkspace(appTemplate.getGUI().getAppPane());
    }

    private boolean promptToSave() throws IOException, URISyntaxException {
        PropertyManager            propertyManager   = PropertyManager.getManager();
        YesNoCancelDialogSingleton yesNoCancelDialog = YesNoCancelDialogSingleton.getSingleton();
        AppMessageDialogSingleton appMessageconfirmation = AppMessageDialogSingleton.getSingleton(); // added for message confirmation


        yesNoCancelDialog.show(propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_TITLE),
                propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_MESSAGE));

        if (yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.YES)) {
            if (workFile != null) {
                saveWork(workFile);
                appMessageconfirmation.show(String.valueOf(SAVE_COMPLETED_TITLE), "Your Work has been Saved Successfully");

            }
            else {
                FileChooser filechooser = new FileChooser();
                String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
                String extension   = propertyManager.getPropertyValue(WORK_FILE_EXT);
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(String.format("%s (*.%s)", description, extension),
                        String.format("*.%s", extension));
                filechooser.setTitle(propertyManager.getPropertyValue(SAVE_WORK_TITLE));
                filechooser.getExtensionFilters().add(extFilter);
                File workFile = filechooser.showSaveDialog(appTemplate.getGUI().getWindow());


                Files.write(Paths.get(workFile.getPath()),"Hello".getBytes(), StandardOpenOption.APPEND);

                appTemplate.getFileComponent().saveData(appTemplate.getDataComponent(), Paths.get(workFile.getAbsolutePath()));


                yesNoCancelDialog.show("Continue","Do you want to Continue Playing the Game ?");

                if(yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.YES) ) {
                    saveWork(workFile);
                }
                else if(yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.NO)){
                    saveWork(workFile);
                    appMessageconfirmation.show(String.valueOf(SAVE_COMPLETED_TITLE), "Your Work has been Saved Successfully");
                }
                else{
                    appMessageconfirmation.show("Enjoy", "Please Enjoy Playing Hangman !");
                }

                /**
                 *  URL throws the error the work folder not found
                 *
                 *
                URL  workDirURL  = AppTemplate.class.getClassLoader().getResource(selectedFile.getAbsolutePath());
                if (workDirURL == null)
                    throw new FileNotFoundException("Work folder not found under resources.");
                File filePath= new File(workDirURL.getFile());
                Path newFilePath= filePath.toPath();
                save(newFilePath);
                */

                if (workFile != null) {
                    saveWork(workFile);
                }
            }
        }
        return !yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.CANCEL);
    }

}
