import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.scene.layout.Region;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;

import javax.swing.text.Style;

/**
 * Created by Richard and Ricardo on 5/3/17.
 */
public class BracketPane extends BorderPane {

        /**
         * Reference to the graphical representation of the nodes within the bracket.
         */
        private static ArrayList<BracketNode> nodes;
        /**
         * Used to initiate the paint of the bracket nodes
         */
        private static boolean isTop = true;
        /**
         * Maps the text "buttons" to it's respective grid-pane
         */
        private HashMap<StackPane, Pane> panes;
        /**
         * Reference to the current bracket.
         */
        private Bracket currentBracket;
        /**
         * Reference to active subtree within current bracket.
         */
        private int displayedSubtree;
        /**
         * Keeps track of whether or not bracket has been finalized.
         */
        private boolean finalized;
        /**
         * Important logical simplification for allowing for code that is easier
         * to maintain.
         */
        private HashMap<BracketNode, Integer> bracketMap = new HashMap<>();
        /**
         * Reverse of the above;
         */
        private HashMap<Integer, BracketNode> nodeMap = new HashMap<>();

        //The buttons to choose a bracket (East, West, Full etc.)
        private ArrayList<StackPane> buttons;

        /**
         * Clears the entries of a team future wins
         *
         * @param treeNum
         */
        private void clearAbove(int treeNum) {
                int nextTreeNum = (treeNum - 1) / 2;
                if (!nodeMap.get(nextTreeNum).getName().isEmpty()) {
                        nodeMap.get(nextTreeNum).setName("");
                        clearAbove(nextTreeNum);
                }
        }
        
        
        public void clear(){
            clearSubtree(displayedSubtree);
        }

        /**
         * Handles clicked events for BracketNode objects
         */
        private EventHandler<MouseEvent> clicked = mouseEvent -> {
                //conditional added by matt 5/7 to differentiate between left and right mouse click
                if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                        BracketNode n = (BracketNode) mouseEvent.getSource();
                        int treeNum = bracketMap.get(n);
                        int nextTreeNum = (treeNum - 1) / 2;
                        if (!nodeMap.get(nextTreeNum).getName().equals(n.getName())) {
                                currentBracket.removeAbove((nextTreeNum));
                                clearAbove(treeNum);
                                nodeMap.get((bracketMap.get(n) - 1) / 2).setName(n.getName());
                                currentBracket.moveTeamUp(treeNum);
                        }
                }
                //added by matt 5/7, shows the teams info if you right click
                else if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
                        String text = "";
                        String logoRef = "";
                        BracketNode n = (BracketNode) mouseEvent.getSource();
                        int treeNum = bracketMap.get(n);
                        String displayName = currentBracket.getBracket().get(treeNum);
                        try {
                                TournamentInfo info = new TournamentInfo();
                                Team t = info.getTeam(displayName);
                                logoRef = t.getLogoRef();
                                //by Tyler - added the last two pieces of info to the pop up window
                                text += "Team: " + t.getFullName() + " | Ranking: " + t.getRanking()
                                        + "\nMascot: " + t.getNickname() + "\nInfo: " + t.getInfo()
                                        + "\nAverage Offensive PPG: " + t.getOffensePPG()
                                        + "\nAverage Defensive PPG: "+ t.getDefensePPG();
                        } catch (IOException e) {//if for some reason TournamentInfo is not working, it
                                // will display info not found
                                text += "Info for " + displayName + "not found";
                        }
                        //create a popup with the team info

                        /*
                        Tooltip tooltip = new Tooltip();
                        tooltip.setText(text);
                        tooltip.setGraphic(new ImageView(this.getClass().getResource("Icons/"+logoRef).toString()));
                         */

                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.CLOSE);
                        alert.setTitle("March Madness Bracket Simulator");
                        alert.setHeaderText(null);

                        alert.setGraphic(new ImageView(this.getClass().getResource("Icons/"+logoRef).toString()));

                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        alert.showAndWait();
                }
        };
        /**
         * Handles mouseEntered events for BracketNode objects
         */
        private EventHandler<MouseEvent> enter = mouseEvent -> {
                BracketNode tmp = (BracketNode) mouseEvent.getSource();
                tmp.setStyle("-fx-background-color: lightgreen;");
                tmp.setEffect(new InnerShadow(10, Color.LIGHTGREEN));
        };

        /**
         * Handles mouseExited events for BracketNode objects
         */
        private EventHandler<MouseEvent> exit = mouseEvent -> {
                BracketNode tmp = (BracketNode) mouseEvent.getSource();
                tmp.setStyle(null);
                tmp.setEffect(null);

        };

        public GridPane getFullPane() {
                return fullPane;
        }

        private GridPane center;
        private GridPane fullPane;
        private GridPane topLeftPane;
        private GridPane topCenterPane;
        private GridPane topRightPane;


        /**
         * TODO: Reduce. reuse, recycle!
         * Initializes the properties needed to construct a bracket.
         */
        public BracketPane(Bracket currentBracket) {
                displayedSubtree=0;
                this.currentBracket = currentBracket;

                bracketMap = new HashMap<>();
                nodeMap = new HashMap<>();
                panes = new HashMap<>();
                nodes = new ArrayList<>();
                ArrayList<Root> roots = new ArrayList<>();

                center = new GridPane();

                buttons = new ArrayList<>();
                buttons.add(customButton("EAST"));
                buttons.add(customButton("WEST"));
                buttons.add(customButton("MIDWEST"));
                buttons.add(customButton("SOUTH"));
                buttons.add(customButton("FULL"));

                ArrayList<GridPane> gridPanes = new ArrayList<>();

                for (int m = 0; m < buttons.size() - 1; m++) {
                        roots.add(new Root(3 + m));
                        panes.put(buttons.get(m), roots.get(m));
                }
                Pane finalPane = createFinalFour();
                //buttons.add(customButton("FINAL"));
                //panes.put(buttons.get(5), finalPane);
                fullPane = new GridPane();
                GridPane gp1 = new GridPane();
                gp1.add(roots.get(0), 0, 0);
                gp1.add(roots.get(1), 0, 1);
                GridPane gp2 = new GridPane();
                gp2.add(roots.get(2), 0, 0);
                gp2.add(roots.get(3), 0, 1);
                gp2.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                fullPane.add(gp1, 0, 1);
                fullPane.add(finalPane, 1, 1, 1, 2);
                fullPane.add(gp2, 2, 1);
                fullPane.setAlignment(Pos.CENTER);
                panes.put(buttons.get((buttons.size() - 1)), fullPane);
                finalPane.toBack();

                topLeftPane = new GridPane();
                topCenterPane = new GridPane();
                topRightPane = new GridPane();

                topLeftPane.add(new Label("ROUND 1"), 0, 0);
                topLeftPane.add(new Label("\tROUND 2\t"), 1, 0);
                topLeftPane.add(new Label("\tSWEET 16\t"), 2, 0);
                topLeftPane.add(new Label("\tELITE 8\t"), 3, 0);
                topLeftPane.add(new Label("\tFINAL FOUR\t"), 4, 0);
                topCenterPane.add(new Label("\tCHAMPIONSHIP\t"), 0, 0);
                topRightPane.add(new Label("\tFINAL FOUR\t"), 0, 0);
                topRightPane.add(new Label("\tELITE 8\t"),1,0);
                topRightPane.add(new Label("\tSWEET 16\t"), 2, 0);
                topRightPane.add(new Label("\tROUND 2\t"), 3, 0);
                topRightPane.add(new Label("\tROUND 1\t"), 4, 0);

                fullPane.add(topLeftPane, 0,0);
                fullPane.add(topCenterPane, 1,0);
                fullPane.add(topRightPane, 2,0);

                topLeftPane.setStyle(" -fx-font-family: Futura; -fx-background-color: lightgreen;" +
                        "-fx-text-fill: #18284a; -fx-alignment:center;");
                topCenterPane.setStyle(" -fx-font-family: Futura; -fx-background-color: lightgreen;" +
                        "-fx-text-fill: #18284a; -fx-alignment:center;");
                topRightPane.setStyle(" -fx-font-family: Futura; -fx-background-color: lightgreen;" +
                        "-fx-text-fill: #18284a; -fx-alignment:center;");

                // Initializes the button grid
                GridPane buttonGrid = new GridPane();
                for (int i = 0; i < buttons.size(); i++)
                        buttonGrid.add(buttons.get(i), 0, i);
                buttonGrid.setAlignment(Pos.CENTER);

                // set default center to the button grid
                this.setCenter(buttonGrid);

                //For all buttons set its functionality
                for(int i = 0; i < buttons.size(); i++){
                StackPane button = buttons.get(i);
                        button.setStyle("-fx-background-color: #8bc4de; -fx-font-family: Futura;");
                        button.setOnMouseEntered(mouseEvent -> {
                                button.setStyle("-fx-background-color: LIGHTGREEN; -fx-font-family: Futura;");
                                button.setEffect(new InnerShadow(10, Color.LIGHTGREEN));
                        });
                        button.setOnMouseExited(mouseEvent -> {
                                button.setStyle("-fx-background-color: #8bc4de; -fx-font-family: Futura;");
                                button.setEffect(null);
                        });

                        //What happens when click happens in button
                        button.setOnMouseClicked(mouseEvent -> {
                                setVisiblePane(buttons.indexOf(button));
                                displayedSubtree=buttons.indexOf(button)==7?0:buttons.indexOf(button)+3;
                                if(buttons.indexOf(button) == 4) {
                                        MarchMadnessGUI.getButton().setDisable(true);
                                }
                                else
                                        MarchMadnessGUI.getButton().setDisable(false);

                        });
                }
        }

        /**
         * Method sets the visible pane on command (Either of the 4 little or the full pane)
         */
        public void setVisiblePane(int index){
                setCenter(null);                         //Center is the grid pane
                center.add(new ScrollPane(panes.get(buttons.get(index))), 0, 0);
                center.setAlignment(Pos.CENTER);
                setCenter(center);
        }

        public int getDisplayedSubtree() {
                return displayedSubtree;
        }

        /**
         * Method to add a triangle in the screen
         * Triangle appears when FULL bracket is visible only. It is centered to the screen.
         */
        private void addTriangle(Pane finalPane) {
                Polygon triangle1 = new Polygon();
                Polygon triangle2 = new Polygon();
                triangle1.getPoints().addAll(new Double[]{
                        finalPane.getMinWidth()/ 2.0, 150.0,
                        finalPane.getMinWidth(), 0.0,
                        0.0, 0.0}
                        );
                triangle1.setFill(Color.LIGHTGREEN);
                triangle2.getPoints().addAll(new Double[]{
                        finalPane.getMinWidth()/ 2.0, 140.0,
                        finalPane.getMinWidth() - 10.0, 0.0,
                        10.0, 0.0}
                );
                triangle2.setFill(Color.rgb(24, 40, 74));
                finalPane.getChildren().add(triangle1);
                finalPane.getChildren().add(triangle2);

                Label label1 = new Label("\n\n2017 NCAA TOURNAMENT");
                Label label2 = new Label("\n\n\nBRACKET");
                String style = " -fx-font-family: Futura; -fx-text-fill: #ffffff; -fx-font-scale: 15;";
                label1.setStyle(style);
                label2.setStyle(style);
                label1.setMinWidth(finalPane.getMinWidth());
                label2.setMinWidth(finalPane.getMinWidth());
                finalPane.getChildren().add(label1);
                finalPane.getChildren().add(label2);
                label1.setAlignment(Pos.TOP_CENTER);
                label2.setAlignment(Pos.TOP_CENTER);
        }

        /**
         * Helpful method to retrieve our magical numbers
         *
         * @param root the root node (3,4,5,6)
         * @param pos  the position in the tree (8 (16) , 4 (8) , 2 (4) , 1 (2))
         * @return The list representing the valid values.
         */
        public ArrayList<Integer> helper(int root, int pos) {
                ArrayList<Integer> positions = new ArrayList<>();
                int base = 0;
                int tmp = (root * 2) + 1;
                if (pos == 8) base = 3;
                else if (pos == 4) base = 2;
                else if (pos == 2) base = 1;
                for (int i = 0; i < base; i++) tmp = (tmp * 2) + 1;
                for (int j = 0; j < pos * 2; j++) positions.add(tmp + j);
                return positions; //                while ((tmp = ((location * 2) + 1)) <= 127) ;
        }

        /**
         * Sets the current bracket to,
         *
         * @param target The bracket to replace currentBracket
         */
        public void setBracket(Bracket target) {
                currentBracket = target;
        }

        /**
         * Clears the sub tree from,
         *
         * @param position The position to clear after
         */
        public void clearSubtree(int position) {
                currentBracket.resetSubtree(position);
        }

        /**
         * Resets the bracket-display
         */
        public void resetBracket() {
                currentBracket.resetSubtree(0);
        }

        /**
         * Requests a message from current bracket to tell if the bracket
         * has been completed.
         *
         * @return True if completed, false otherwise.
         */
        public boolean isComplete() {
                return currentBracket.isComplete();
        }

        /**
         * @return true if the current-bracket is complete and the value
         * of finalized is also true.
         */
        public boolean isFinalized() {
                return currentBracket.isComplete() && finalized;
        }

        /**
         * @param isFinalized The value to set finalized to.
         */
        public void setFinalized(boolean isFinalized) {
                finalized = isFinalized && currentBracket.isComplete();
        }

        /**
         * Returns a custom "Button" with specified
         *
         * @param name The name of the button
         * @return pane The stack-pane "button"
         */
        private StackPane customButton(String name) {
                StackPane pane = new StackPane();
                Rectangle r = new Rectangle(100, 50, Color.TRANSPARENT);
                Text t = new Text(name);
                t.setTextAlignment(TextAlignment.CENTER);
                pane.getChildren().addAll(r, t);
                pane.setStyle("-fx-background-color: #8bc4de;");
                return pane;
        }

        public Pane createFinalFour() {
                Pane finalPane = new Pane();
                finalPane.setMinWidth(400.0);
                BracketNode nodeFinal0 = new BracketNode("", 162, 300, 70, 0);
                BracketNode nodeFinal1 = new BracketNode("", 75, 400, 70, 0);
                BracketNode nodeFinal2 = new BracketNode("", 250, 400, 70, 0);
                nodeFinal0.setName(currentBracket.getBracket().get(0));
                nodeFinal1.setName(currentBracket.getBracket().get(1));
                nodeFinal2.setName(currentBracket.getBracket().get(2));
                finalPane.getChildren().add(nodeFinal0);
                finalPane.getChildren().add(nodeFinal1);
                finalPane.getChildren().add(nodeFinal2);
                addTriangle(finalPane);

                bracketMap.put(nodeFinal1, 1);
                bracketMap.put(nodeFinal2, 2);
                bracketMap.put(nodeFinal0, 0);
                nodeMap.put(1, nodeFinal1);
                nodeMap.put(2, nodeFinal2);
                nodeMap.put(0, nodeFinal0);

                nodeFinal0.setOnMouseClicked(clicked);
                nodeFinal0.setOnMouseDragEntered(enter);
                nodeFinal0.setOnMouseDragExited(exit);

                nodeFinal1.setOnMouseClicked(clicked);
                nodeFinal1.setOnMouseDragEntered(enter);
                nodeFinal1.setOnMouseDragExited(exit);

                nodeFinal2.setOnMouseClicked(clicked);
                nodeFinal2.setOnMouseDragEntered(enter);
                nodeFinal2.setOnMouseDragExited(exit);
                nodeFinal0.setStyle("-fx-border-color: #18284a");
                nodeFinal1.setStyle("-fx-border-color: #18284a");
                nodeFinal2.setStyle("-fx-border-color: #18284a");
                return finalPane;
        }

        /**
         * Creates the graphical representation of a subtree.
         * Note, this is a vague model. TODO: MAKE MODULAR
         */
        private class Root extends Pane {

                private int location;

                public Root(int location) {
                        this.location = location;
                        createVertices(420, 200, 100, 20, 0, 0);
                        createVertices(320, 119, 100, 200, 1, 0);
                        createVertices(220, 60, 100, 100, 2, 200);
                        createVertices(120, 35, 100, 50, 4, 100);
                        createVertices(20, 25, 100, 25, 8, 50);
                        for (BracketNode n : nodes) {
                                n.setOnMouseClicked(clicked);
                                n.setOnMouseEntered(enter);
                                n.setOnMouseExited(exit);
                        }
                }

                /**
                 * The secret sauce... well not really,
                 * Creates 3 lines in appropriate location unless it is the last line.
                 * Adds these lines and "BracketNodes" to the Pane of this inner class
                 */
                private void createVertices(int iX, int iY, int iXO, int iYO, int num, int increment) {
                        int y = iY;
                        if (num == 0 && increment == 0) {
                                BracketNode last = new BracketNode("", iX, y - 20, iXO, 20);
                                nodes.add(last);
                                getChildren().addAll(new Line(iX, iY, iX + iXO, iY), last);
                                last.setName(currentBracket.getBracket().get(location));
                                bracketMap.put(last, location);
                                nodeMap.put(location, last);
                        } else {
                                ArrayList<BracketNode> aNodeList = new ArrayList<>();
                                for (int i = 0; i < num; i++) {
                                        Point2D tl = new Point2D(iX, y);
                                        Point2D tr = new Point2D(iX + iXO, y);
                                        Point2D bl = new Point2D(iX, y + iYO);
                                        Point2D br = new Point2D(iX + iXO, y + iYO);
                                        BracketNode nTop = new BracketNode("", iX, y - 20, iXO, 20);
                                        aNodeList.add(nTop);
                                        nodes.add(nTop);
                                        BracketNode nBottom = new BracketNode("", iX, y + (iYO - 20), iXO, 20);
                                        aNodeList.add(nBottom);
                                        nodes.add(nBottom);
                                        Line top = new Line(tl.getX(), tl.getY(), tr.getX(), tr.getY());
                                        Line bottom = new Line(bl.getX(), bl.getY(), br.getX(), br.getY());
                                        Line right = new Line(tr.getX(), tr.getY(), br.getX(), br.getY());
                                        getChildren().addAll(top, bottom, right, nTop, nBottom);
                                        isTop = !isTop;
                                        y += increment;
                                }
                                ArrayList<Integer> tmpHelp = helper(location, num);
                                for (int j = 0; j < aNodeList.size(); j++) {
                                        aNodeList.get(j).setName(currentBracket.getBracket().get(tmpHelp.get(j)));
                                        bracketMap.put(aNodeList.get(j), tmpHelp.get(j));
                                        nodeMap.put(tmpHelp.get(j), aNodeList.get(j));
                                }
                        }

                }
        }

        /**
         * The BracketNode model for the Graphical display of the "Bracket"
         */
        private class BracketNode extends Pane {
                private String displayName;
                private Rectangle rect;
                private Label name;

                /**
                 * Creates a BracketNode with,
                 *
                 * @param displayName The name if any
                 * @param x        The starting x location
                 * @param y        The starting y location
                 * @param rX       The width of the rectangle to fill pane
                 * @param rY       The height of the rectangle
                 */
                public BracketNode(String displayName, int x, int y, int rX, int rY) {
                        this.setLayoutX(x);
                        this.setLayoutY(y);
                        this.setMaxSize(rX, rY);
                        this.displayName = displayName;
                        rect = new Rectangle(rX, rY);
                        rect.setFill(Color.TRANSPARENT);
                        name = new Label(displayName);
                        name.setTranslateX(5);
                        name.setStyle("-fx-font-family: Futura; -fx-text-fill: #16284f");
                        getChildren().addAll(name, rect);
                        name.setTooltip(new Tooltip("hover!"));
                }

                /**
                 * @return displayName The teams name.
                 */
                public String getName() {
                        return displayName;
                }

                /**
                 * @param displayName The name to assign to the node.
                 */
                public void setName(String displayName) {
                        this.displayName = displayName;
                        name.setText(displayName);
                }

                private void setUpToolTip() {
                        String infoText = "";
                        String logoRef = "";
                        try {
                                TournamentInfo info = new TournamentInfo();
                                Team t = info.getTeam(displayName);
                                logoRef = t.getLogoRef();
                                //by Tyler - added the last two pieces of info to the pop up window
                                infoText += "Team: " + t.getFullName() + " | Ranking: " + t.getRanking()
                                        + "\nMascot: " + t.getNickname() + "\nInfo: " + t.getInfo()
                                        + "\nAverage Offensive PPG: " + t.getOffensePPG()
                                        + "\nAverage Defensive PPG: " + t.getDefensePPG();
                        } catch (IOException e) {//if for some reason TournamentInfo is not working, it
                                // will display info not found
                                infoText += "Info for " + displayName + "not found";
                        }
                        Tooltip tooltip = new Tooltip();
                        tooltip.setText(infoText);
                        tooltip.setGraphic(new ImageView(this.getClass().getResource("Icons/"+logoRef).toString()));
                }
        }
}
