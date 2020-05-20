
package log350.example.example6;

import java.util.ArrayList;

//import java.util.List;

import android.content.Context;
//import android.graphics.Matrix;
import android.graphics.Canvas;
//import android.graphics.Rect;
//import android.graphics.Path;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


// This class stores the current position of a finger,
// as well as the history of previous positions of that finger
// during its drag.
//
// An instance of this class is created when a finger makes contact
// with the multitouch surface.  The instance stores all
// subsequent positions of the finger, and is destroyed
// when the finger is lifted off the multitouch surface.

/*****************************************************************************
 *
 *                      CLASS: MyCursor
 *
 *****************************************************************************/
class MyCursor {

    // Each finger in contact with the multitouch surface is given
    // a unique id by the framework (or computing platform).
    // There is no guarantee that these ids will be consecutive nor increasing.
    // For example, when two fingers are in contact with the multitouch surface,
    // their ids may be 0 and 1, respectively,
    // or their ids may be 14 and 9, respectively.
    public int id; // identifier

    // This stores the history of positions of the "cursor" (finger)
    // in pixel coordinates.
    // The first position is where the finger pressed down,
    // and the last position is the current position of the finger.
    private ArrayList<Point2D> positions = new ArrayList<Point2D>();


    // These are used to store what the cursor is being used for.
    public static final int TYPE_DRAGGING = 0; // the finger can be used for dragging objects, zooming, drawing a lasso, ...
//    public static final int TYPE_BUTTON = 1; // the finger is pressing a virtual button
    public static final int TYPE_IGNORE = 2; // the finger should not be there and will be ignored
    public int type = TYPE_IGNORE;

    public MyCursor(int id, float x, float y) {
        this.id = id;
        positions.add(new Point2D(x, y));
    }

    public ArrayList<Point2D> getPositions() { return positions; }

    public void addPosition(Point2D p) { positions.add(p); }

    public Point2D getFirstPosition() {
        if (positions == null || positions.size() < 1)
            return null;
        return positions.get(0);
    }

    public Point2D getCurrentPosition() {
        if (positions == null || positions.size() < 1)
            return null;
        return positions.get(positions.size() - 1);
    }

    public Point2D getPreviousPosition() {
        if (positions == null || positions.size() == 0)
            return null;
        if (positions.size() == 1)
            return positions.get(0);
        return positions.get(positions.size() - 2);
    }

    public int getType() { return type; }

    public void setType(int type) { this.type = type; }
}

// This stores a set of instances of MyCursor.
// Each cursor can be identified by its id,
// which is assigned by the framework or computing platform.
// Each cursor can also be identified by its index in this class's container.
// For example, if an instance of this class is storing 3 cursors,
// their ids may be 2, 18, 7,
// but their indices should be 0, 1, 2.

/*****************************************************************************
 *
 *                      CLASS: CursorContainer
 *
 *****************************************************************************/
class CursorContainer {

    private ArrayList<MyCursor> cursors = new ArrayList<MyCursor>();

    /*** Méthode qui permet de récupérer la liste des curseurs ***/
    public ArrayList<MyCursor> getCursors() { return cursors; }

    /*** Méthode qui permet de vider la liste des curseurs ***/
    public void clearCursors() { cursors.clear(); }

    public int getNumCursors() { return cursors.size(); }

    public MyCursor getCursorByIndex(int index) { return cursors.get(index); }

    public int findIndexOfCursorById(int id) {
        for (int i = 0; i < cursors.size(); ++i) {
            if (cursors.get(i).id == id)
                return i;
        }
        return -1;
    }

    public MyCursor getCursorById(int id) {
        int index = findIndexOfCursorById(id);
        return (index == -1) ? null : cursors.get(index);
    }

    // Returns the number of cursors that are of the given type.
    public int getNumCursorsOfGivenType(int type) {
        int num = 0;
        for (int i = 0; i < cursors.size(); ++i) {
            if (cursors.get(i).getType() == type)
                num++;
        }
        return num;
    }

    // Returns the (i)th cursor of the given type,
    // or null if no such cursor exists.
    // Can be used for retrieving both cursors of type TYPE_DRAGGING, for example,
    // by calling getCursorByType( MyCursor.TYPE_DRAGGING, 0 )
    // and getCursorByType( MyCursor.TYPE_DRAGGING, 1 ),
    // when there may be cursors of other type present at the same time.
    public MyCursor getCursorByType(int type, int i) {
        for (int ii = 0; ii < cursors.size(); ++ii) {
            if (cursors.get(ii).getType() == type) {
                if (i == 0)
                    return cursors.get(ii);
                else
                    i--;
            }
        }
        return null;
    }

    // Returns index of updated cursor.
    // If a cursor with the given id does not already exist, a new cursor for it is created.
    public int updateCursorById(int id, float x, float y) {
        Point2D updatedPosition = new Point2D(x, y);
        int index = findIndexOfCursorById(id);
        if (index == -1) {
            cursors.add(new MyCursor(id, x, y));
            index = cursors.size() - 1;
        }
        MyCursor c = cursors.get(index);
        if (!c.getCurrentPosition().equals(updatedPosition)) {
            c.addPosition(updatedPosition);
        }
        return index;
    }

    public void removeCursorByIndex(int index) {
        cursors.remove(index);
    }
}

/*****************************************************************************
 *
 *                      CLASS: DrawingView
 *
 *****************************************************************************/
public class DrawingView extends View {

    private Paint paint = new Paint();
    private Paint paintTouch = new Paint();
    private GraphicsWrapper gw = new GraphicsWrapper();

    private ShapeContainer shapeContainer = new ShapeContainer();
    private ArrayList<Shape> selectedShapes = new ArrayList<Shape>();
    private CursorContainer cursorContainer = new CursorContainer();

    static final int MODE_NEUTRAL = 0; // the default mode
    static final int MODE_CAMERA_MANIPULATION = 1; // the user is panning/zooming the camera
    static final int MODE_SHAPE_MANIPULATION = 2; // the user is translating/rotating/scaling a shape
    static final int MODE_LASSO = 3; // the user is drawing a lasso to select shapes
    static final int MODE_LASSO_MANIPULATION = 4; // the user is drawing a lasso to select shapes
    private int currentMode = MODE_NEUTRAL;

    /*****************************************************************************
     * AJOUTS CONSTANTES: Numérotation des options Effacer, Encadrer et Créer
     *****************************************************************************/
    static final int MODE_EFFACER = 5; // l'Utilisateur supprime une forme sélectionnée
    static final int MODE_ENCADRER = 6; // l'Utilisateur est entrain de recadrer la fenêtre principale
    static final int MODE_CREER = 7; // l'Utilisateur est entrain de créer un nouvelle forme

    // This is only used when currentMode==MODE_SHAPE_MANIPULATION, otherwise it is equal to -1.
    // Index est à -1 lorsqu'on sélectionne une position dans le vide (UI).
    private int indexOfShapeBeingManipulated = -1;

    // This is only used when currentMode==MODE_LASSO_MANIPULATION, otherwise it is equal to -1.
    // Indique que le lasso sera manipulé
    private int indexOfLassoBeingManipulated = -1;

    // Partie rouge du lasso indiquant le regroupement des formes
    private AlignedRectangle2D lassoRectangle = new AlignedRectangle2D();

    /*****************************************************************************
     * CREER MyButton:  Boutons des options Lasso, Effacer, Encadrer et Créer.
     *****************************************************************************/
    private MyButton lassoButton = new MyButton("Lasso", 10, 70, 200, 200);
    private MyButton effacerButton = new MyButton("Effacer", 10, 300, 200, 200);
    private MyButton encadrerButton = new MyButton("Encadrer", 10, 530, 200, 200);
    private MyButton creerButton = new MyButton("Créer", 10, 760, 200, 200);

    /**
     * Garde en memoire les points pour la creation de formes
     **/
    private ArrayList<Point2D> arrayListPoints = new ArrayList<Point2D>();

    private OnTouchListener touchListener;

    public DrawingView(Context context) {
        super(context);

        setFocusable(true);
        setFocusableInTouchMode(true);

        this.setOnTouchListener(getTouchListener());
        this.setBackgroundColor(Color.WHITE);

        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);

        /** Creation et ajout par defaut d'un triangle **/
        arrayListPoints.add(new Point2D(1100, 300));
        arrayListPoints.add(new Point2D(1200, 650));
        arrayListPoints.add(new Point2D(1300, 350));
        shapeContainer.addShape(arrayListPoints);
        arrayListPoints.clear();

        /** Creation et ajout par defaut d'un carré **/
        arrayListPoints.add(new Point2D(600, 100));
        arrayListPoints.add(new Point2D(900, 100));
        arrayListPoints.add(new Point2D(900, 300));
        arrayListPoints.add(new Point2D(600, 300));
        shapeContainer.addShape(arrayListPoints);
        arrayListPoints.clear();

        /** Creation et ajout par defaut d'une forme pentagonale **/
        arrayListPoints.add(new Point2D(550, 400));
        arrayListPoints.add(new Point2D(850, 400));
        arrayListPoints.add(new Point2D(1050, 600));
        arrayListPoints.add(new Point2D(950, 700));
        arrayListPoints.add(new Point2D(750, 700));
        shapeContainer.addShape(arrayListPoints);
        arrayListPoints.clear();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // The view is constantly redrawn by this method
        gw.set(paint, canvas);
        gw.set(paintTouch, canvas);

        /*** Couleur du Background UI ***/
        gw.clear(0.3f, 0.3f, 0.3f);
        gw.setCoordinateSystemToWorldSpaceUnits();
        gw.setLineWidth(1);

        // draw a polygon around the currently selected shapes
        if (selectedShapes.size() > 0) {
            ArrayList<Point2D> points = new ArrayList<Point2D>();
            lassoRectangle = new AlignedRectangle2D();

            for (Shape s : selectedShapes) {
                for (Point2D p : s.getPoints()) {
                    points.add(p);
                    lassoRectangle.bound(p);
                }
            }
            points = Point2DUtil.computeConvexHull(points);
            points = Point2DUtil.computeExpandedPolygon(points, lassoRectangle.getDiagonal().length() / 30);

            gw.setColor(1.0f, 0.0f, 0.0f, 0.8f);
            gw.fillPolygon(points);
        }

        // draw all the shapes
        shapeContainer.draw(gw, indexOfShapeBeingManipulated);

        gw.setCoordinateSystemToPixels();

        /*****************************************************************************
         * AJOUTS MyButton:  Boutons des options Lasso, Effacer, Encadrer et Créer.
         *****************************************************************************/
        creerButton.draw(gw, currentMode == MODE_CREER);
        lassoButton.draw(gw, currentMode == MODE_LASSO);
        effacerButton.draw(gw, currentMode == MODE_EFFACER);
        encadrerButton.draw(gw, currentMode == MODE_ENCADRER);

        if (currentMode == MODE_LASSO) {
            MyCursor lassoCursor = cursorContainer.getCursorByType(MyCursor.TYPE_DRAGGING, 0);
            if (lassoCursor != null) {
                gw.setColor(1.0f, 0.0f, 0.0f, 0.5f);
                gw.fillPolygon(lassoCursor.getPositions());
            }
        }
        if (cursorContainer.getNumCursors() > 0) {
            gw.setFontHeight(30);
            gw.setLineWidth(2);
            gw.setColor(1.0f, 1.0f, 1.0f);
            gw.drawString(10, 50, "[" + cursorContainer.getNumCursors() + " contacts]");

            /*** Appel la methode qui permet d'afficher les points de contacts a l'ecran (UI) ***/
            multiTouchCursor(canvas);
        }
    }

    /**********************************************************************
     * METHOD - MULTITOUCH CURSOR:
     *
     * 		Creation de cercles (points) qui suivent dynamiquement les doigts de
     * 		l'utilisateur qui sont en contact avec l'écran (UI).
     * 		Bref, c'est à l'aide d'une boucle (nb de contacts) qu'on recupere
     * 		la position en X et Y de chaque 'cursor' d'un ArrayList<MyCursor>
     * 		pour créer des cercles.
     *
     **********************************************************************/
    private void multiTouchCursor(Canvas canvas) {

        /*** Boucle en fonction du nombre de contacts avec l'ecran ***/
        for (int i = 0; i < cursorContainer.getNumCursors(); i++) {

            /*** Point initial ***/
            gw.setColor(1.0f, 0.0f, 0.0f, 0.6f);
            int initTouchX = (int) cursorContainer.getCursors().get(i).getFirstPosition().x();
            int initTouchY = (int) cursorContainer.getCursors().get(i).getFirstPosition().y();
            canvas.drawCircle(initTouchX, initTouchY, 20, paintTouch);

            /*** Point dynamique ***/
            gw.setColor(1.0f, 0.0f, 0.0f, 0.4f);
            int followTouchX = (int) cursorContainer.getCursors().get(i).getCurrentPosition().x();
            int followTouchY = (int) cursorContainer.getCursors().get(i).getCurrentPosition().y();
            canvas.drawCircle(followTouchX, followTouchY, 60, paintTouch);

            /*** Dessine la ligne qui relie le point initial et le point dynamique ***/
            gw.setColor(1.0f, 0.0f, 0.0f, 1.0f);
            canvas.drawLine(initTouchX, initTouchY, followTouchX, followTouchY, paintTouch);
        }

        /*** Retire les cercles qui suivent les doigts si le user ne touche pas a l'ecran ***/
        if (cursorContainer.getNumCursors() == 0) {
            cursorContainer.clearCursors();
        }
    }

    /**********************************************************************
     * RÉSUMER:
     *
     * 		Permet de supporter l'ajout d'une forme. La liste de points
     * 	    gardés en mémoire permet la création d'une forme 2D.
     * 	    À la toute fin, on vide la liste de points pour la création
     * 	    d'une prochaine forme.
     *
     **********************************************************************/
    private void cursorCreateShape() {

        /** Boucle autour du nombre de curseurs **/
        for (int i = 0; i < cursorContainer.getNumCursors(); i++) {
            arrayListPoints.add(gw.convertPixelsToWorldSpaceUnits(cursorContainer.getCursorByIndex(i).getCurrentPosition()));

            /** Puisqu'appuyer sur le bouton Créer ajoute un point, il faut le retirer **/
           if (i == 0) arrayListPoints.remove(0);
        }
        /** Conversion des points en une forme qu'on ajoute dans la liste de formes **/
        shapeContainer.addShape(Point2DUtil.computeConvexHull(arrayListPoints));
        arrayListPoints.clear();
    }

    /**
     * Returns a listener
     *
     * @return a listener
     */
    private OnTouchListener getTouchListener() {

        if (touchListener == null) {
            touchListener = new OnTouchListener() {

                public boolean onTouch(View v, MotionEvent event) {

                    int type = MotionEvent.ACTION_MOVE;

                    switch (event.getActionMasked()) {

                        case MotionEvent.ACTION_DOWN:
                            type = MotionEvent.ACTION_DOWN;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            type = MotionEvent.ACTION_MOVE;
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_POINTER_UP:
                        case MotionEvent.ACTION_CANCEL:
                            type = MotionEvent.ACTION_UP;
                            break;
                    }

                    int id = event.getPointerId(event.getActionIndex());
                    float x = event.getX(event.getActionIndex());
                    float y = event.getY(event.getActionIndex());

                    // Find the cursor that corresponds to the event id, if such a cursor already exists.
                    // If no such cursor exists, the below index will be -1, and the reference to cursor will be null.
                    int cursorIndex = cursorContainer.findIndexOfCursorById(id);
                    MyCursor cursor = (cursorIndex == -1) ? null : cursorContainer.getCursorByIndex(cursorIndex);

                    if (cursor == null) {

                        // The event does not correspond to any existing cursor.
                        // In other words, this is a new finger touching the screen.
                        // The event is probably of type DOWN.
                        // A new cursor will need to be created for the event.
                        if (type == MotionEvent.ACTION_UP) {
                            // This should never happen, but if it does, just ignore the event.
                            return true;
                        }
                        type = MotionEvent.ACTION_DOWN;

                        // Cause a new cursor to be created to keep track of this event id in the future
                        cursorIndex = cursorContainer.updateCursorById(id, x, y);
                        cursor = cursorContainer.getCursorByIndex(cursorIndex);

                        // we will set the type of the cursor later, by calling cursor.setType( MyCursor.TYPE_... );
                    } else {
                        // The event corresponds to an already existing cursor
                        // (and the cursor was probably created during an earlier event of type TOUCH_EVENT_DOWN).
                        // The current event is probably of type MOVE or UP.

                        cursorContainer.updateCursorById(id, x, y);

                        if (type == MotionEvent.ACTION_MOVE) {
                            // Other fingers may have also moved, and there new positions are available in the event passed to us.
                            // For safety, we update all cursors now with their latest positions.
                            for (int i = 0; i < event.getPointerCount(); ++i) {
                                int tmp_id = event.getPointerId(i);
                                cursorContainer.updateCursorById(tmp_id, event.getX(i), event.getY(i));
                            }
                        }
                    }

                    switch (currentMode) {

                        case MODE_NEUTRAL:

                            if (cursorContainer.getNumCursors() == 1) {
                                Point2D p_pixels = new Point2D(x, y);
                                Point2D p_world = gw.convertPixelsToWorldSpaceUnits(p_pixels);

                                /** Identifier l'ID (index) de la Forme sélectionnée **/
                                indexOfShapeBeingManipulated = shapeContainer.indexOfShapeContainingGivenPoint(p_world);

                                /** Selection a l'interieur du Lasso **/
                                if      (lassoRectangle.contains(p_world))  indexOfLassoBeingManipulated = 1;
                                else if (indexOfShapeBeingManipulated >= 0) Log.i("INDEX", "ID Forme sélectionnée = " + String.valueOf(indexOfShapeBeingManipulated));
                                else if (indexOfLassoBeingManipulated >= 0) Log.i("LASSO", "Lasso sélectionnée");
                                else                                        Log.i("INDEX", "Aucune Forme sélectionnée");

                                // Removes the lasso if a click on an empty space occurs
                                if (indexOfShapeBeingManipulated < 0 && indexOfLassoBeingManipulated < 0
                                        && !effacerButton.contains(p_pixels) && !encadrerButton.contains(p_pixels)
                                        && !creerButton.contains(p_pixels)) {
                                    selectedShapes.clear();
                                    lassoRectangle = new AlignedRectangle2D();
                                }

                                /**********************************************************************
                                 * RÉSUMER:
                                 *
                                 * 		Permet de mettre en surbrillance le bouton "Creer" lorsqu'il
                                 * 		est sélectionné. Active le mode Creer.
                                 *
                                 **********************************************************************/
                                if (creerButton.contains(p_pixels)) {
                                    currentMode = MODE_CREER;
                                    Log.i("OPTION", "Bouton sélectionné: Create");
                                }

                                /**********************************************************************
                                 * RÉSUMER:
                                 *
                                 * 		Permet de mettre en surbrillance le bouton "Lasso" lorsqu'il
                                 * 		est sélectionné. Active le mode Lasso.
                                 *
                                 **********************************************************************/
                                else if (lassoButton.contains(p_pixels)) {
                                    currentMode = MODE_LASSO;
                                    Log.i("OPTION", "Bouton sélectionné: Lasso");
                                }

                                /**********************************************************************
                                 * RÉSUMER:
                                 *
                                 * 		Permet de mettre en surbrillance le bouton "Effacer" lorsqu'il
                                 * 		est sélectionné. Active le mode Effacer.
                                 * 		Principalement inspiré du mode "Lasso" ci-dessus.
                                 *
                                 **********************************************************************/
                                else if (effacerButton.contains(p_pixels)) {
                                    currentMode = MODE_EFFACER;
                                    Log.i("OPTION", "Bouton sélectionné: Effacer");
                                }

                                /**********************************************************************
                                 * RÉSUMER:
                                 *
                                 * 		Permet de mettre en surbrillance le bouton "Encadrer" lorsqu'il
                                 * 		est sélectionné. Active le mode Encadrer.
                                 * 		Principalement inspiré du mode "Lasso".
                                 *
                                 **********************************************************************/
                                else if (encadrerButton.contains(p_pixels)) {
                                    currentMode = MODE_ENCADRER;
                                    Log.i("OPTION", "Bouton sélectionné: Encadrer");
                                }
                                /** On peut manipuler une forme seulement si elle n'est pas dans un Lasso **/
                                else if (indexOfShapeBeingManipulated >= 0 && indexOfLassoBeingManipulated < 1){
                                    currentMode = MODE_SHAPE_MANIPULATION;
                                    cursor.setType(MyCursor.TYPE_DRAGGING);
                                } else if (indexOfLassoBeingManipulated > 0 ) {
                                    currentMode = MODE_LASSO_MANIPULATION;
                                    cursor.setType(MyCursor.TYPE_DRAGGING);
                                } else {
                                    currentMode = MODE_CAMERA_MANIPULATION;
                                    cursor.setType(MyCursor.TYPE_DRAGGING);
                                }
                            }
                            break;

                        case MODE_CAMERA_MANIPULATION:

                            /** Deplacement de la camera avec 1 doigt **/
                            if (cursorContainer.getNumCursors() == 1 && type == MotionEvent.ACTION_MOVE) {
                                MyCursor cursor0 = cursorContainer.getCursorByIndex(0);

                                gw.panBasedOnDisplacementOfOnePoint(
                                        cursor0.getPreviousPosition(),
                                        cursor0.getCurrentPosition());
                            }
                            /** Deplacement et manipulation de la camera avec 2 doigts **/
                            else if (cursorContainer.getNumCursors() == 2 && type == MotionEvent.ACTION_MOVE) {
                                MyCursor cursor0 = cursorContainer.getCursorByIndex(0);
                                MyCursor cursor1 = cursorContainer.getCursorByIndex(1);

                                gw.panAndZoomBasedOnDisplacementOfTwoPoints(
                                        cursor0.getPreviousPosition(),
                                        cursor1.getPreviousPosition(),
                                        cursor0.getCurrentPosition(),
                                        cursor1.getCurrentPosition()
                                );
                            }
                            else if (type == MotionEvent.ACTION_UP) {
                                cursorContainer.removeCursorByIndex(cursorIndex);
                                if (cursorContainer.getNumCursors() == 0) {
                                    currentMode = MODE_NEUTRAL;
                                }
                            }
                            break;

                        case MODE_SHAPE_MANIPULATION:

                            if (type == MotionEvent.ACTION_MOVE && indexOfShapeBeingManipulated >= 0) {

                                /**********************************************************************
                                 * RÉSUMER:
                                 *
                                 * 		Permet de supporter le déplacement d'une forme avec une
                                 * 		sélection de type unique (1 doigt).
                                 * 		Le "if" ci-dessous a principalement été inspiré par le
                                 * 		"else if (cursorContainer.getNumCursors() == 2)" qui permet
                                 * 		de supporter le déplacement et la manipulation des formes
                                 * 		avec deux doigts.
                                 *
                                 **********************************************************************/
                                if (cursorContainer.getNumCursors() == 1) {
                                    MyCursor cursor0 = cursorContainer.getCursorByIndex(0);
                                    Shape shape = shapeContainer.getShape(indexOfShapeBeingManipulated);

                                    Point2DUtil.transformPointsBasedOnDisplacementOfOnePoint(
                                            shape.getPoints(),
                                            gw.convertPixelsToWorldSpaceUnits(cursor0.getPreviousPosition()),
                                            gw.convertPixelsToWorldSpaceUnits(cursor0.getCurrentPosition())
                                    );
                                }

                                /** Déplacement et manipulation d'une forme à deux doigts **/
                                else if (cursorContainer.getNumCursors() == 2) {
                                    MyCursor cursor0 = cursorContainer.getCursorByIndex(0);
                                    MyCursor cursor1 = cursorContainer.getCursorByIndex(1);
                                    Shape shape = shapeContainer.getShape(indexOfShapeBeingManipulated);

                                    Point2DUtil.transformPointsBasedOnDisplacementOfTwoPoints(
                                            shape.getPoints(),
                                            gw.convertPixelsToWorldSpaceUnits(cursor0.getPreviousPosition()),
                                            gw.convertPixelsToWorldSpaceUnits(cursor1.getPreviousPosition()),
                                            gw.convertPixelsToWorldSpaceUnits(cursor0.getCurrentPosition()),
                                            gw.convertPixelsToWorldSpaceUnits(cursor1.getCurrentPosition())
                                    );
                                }
                            } else if (type == MotionEvent.ACTION_UP) {
                                cursorContainer.removeCursorByIndex(cursorIndex);
                                if (cursorContainer.getNumCursors() == 0) {
                                    currentMode = MODE_NEUTRAL;
                                    indexOfShapeBeingManipulated = -1;
                                }
                            }
                            break;

                        case MODE_LASSO:

                            if (type == MotionEvent.ACTION_DOWN) {
                                // there's already a finger dragging out the lasso
                                if (cursorContainer.getNumCursorsOfGivenType(MyCursor.TYPE_DRAGGING) == 1)
                                    cursor.setType(MyCursor.TYPE_IGNORE);
                                else cursor.setType(MyCursor.TYPE_DRAGGING);
                            } else if (type == MotionEvent.ACTION_MOVE) {/* no further updating necessary here*/
                            } else if (type == MotionEvent.ACTION_UP) {

                                if (cursor.getType() == MyCursor.TYPE_DRAGGING) {

                                    /*** Si il y a un lasso et que le user cree un autre lasso, ca va supprimer l'ancien ***/
                                    selectedShapes.clear();

                                    // Need to transform the positions of the cursor from pixels to world space coordinates.
                                    // We will store the world space coordinates in the following data structure.
                                    ArrayList<Point2D> lassoPolygonPoints = new ArrayList<Point2D>();
                                    for (Point2D p : cursor.getPositions())
                                        lassoPolygonPoints.add(gw.convertPixelsToWorldSpaceUnits(p));
                                    for (Shape s : shapeContainer.shapes) {
                                        if (s.isContainedInLassoPolygon(lassoPolygonPoints)) selectedShapes.add(s);
                                    }
                                    if (selectedShapes.size() > 0) Log.i("LASSO", "Nombre de Formes dans Lasso: " + selectedShapes.size());
                                }
                                cursorContainer.removeCursorByIndex(cursorIndex);

                                /** Suivi du relâchement du bouton 'Lasso' **/
                                if (cursorContainer.getNumCursors() == 0) {
                                    currentMode = MODE_NEUTRAL;
                                    lassoRectangle = new AlignedRectangle2D();
                                    Log.i("OPTION", "Retour au mode Neutre");
                                }
                            }
                            break;

                        /**********************************************************************
                         * RÉSUMER:
                         *
                         * 		Permet de supporter la translation de toutes les composantes
                         * 		s'y trouvant dans une sélection(lasso)
                         *
                         **********************************************************************/
                        case MODE_LASSO_MANIPULATION:
                            if (type == MotionEvent.ACTION_MOVE && indexOfLassoBeingManipulated >= 0) {

                                if (cursorContainer.getNumCursors() == 1) {
                                    MyCursor cursor0 = cursorContainer.getCursorByIndex(0);
                                    ArrayList<Point2D> shapePoints = new ArrayList<Point2D>();
                                    for (Shape s : selectedShapes) {
                                        shapePoints.addAll(s.getPoints());
                                    }

                                    Point2DUtil.transformPointsBasedOnDisplacementOfOnePoint(
                                            shapePoints,
                                            gw.convertPixelsToWorldSpaceUnits(cursor0.getPreviousPosition()),
                                            gw.convertPixelsToWorldSpaceUnits(cursor0.getCurrentPosition())
                                    );
                                }
                            } else if (type == MotionEvent.ACTION_UP) {
                                cursorContainer.removeCursorByIndex(cursorIndex);
                                if (cursorContainer.getNumCursors() == 0) {
                                    currentMode = MODE_NEUTRAL;
                                    indexOfShapeBeingManipulated = -1;
                                    indexOfLassoBeingManipulated = -1;
                                }
                            }
                            break;

                        /**********************************************************************
                         * RÉSUMER:
                         *
                         * 		Permet de supporter l'ajout du mode Effacer.
                         * 	    Le mode permet non seulement la suppression d'une forme,
                         * 	    mais aussi supprimer une forme dans un lasso et assurer
                         * 	    le redimmensionnement du lasso de façon dynamique.
                         *
                         **********************************************************************/
                        case MODE_EFFACER:

                            Point2D p_world = gw.convertPixelsToWorldSpaceUnits(new Point2D(x, y));
                            indexOfShapeBeingManipulated = shapeContainer.indexOfShapeContainingGivenPoint(p_world);

                            /** Lorsque le bouton 'Effacer' est enfoncé et qu'on relache la saisie d'une forme à supprimer **/
                            if (type == MotionEvent.ACTION_UP && cursorContainer.getNumCursors() >= 1) {

                                /** Si on sélectionne une forme **/
                                if (indexOfShapeBeingManipulated >= 0) {

                                    /** Suppression de la forme sélectionnée **/
                                    Shape shape = shapeContainer.shapes.get(indexOfShapeBeingManipulated);
                                    shapeContainer.shapes.remove(shape);
                                    selectedShapes.remove(shape);
                                    Log.i("SHAPE", "Forme supprimée");
                                }
                                /** Si la forme est dans un Lasso **/
                                else if (indexOfLassoBeingManipulated == 1) {

                                    /** Suppression de la forme et ajuste dynamiquement le Lasso **/
                                    for (int i = 0; i < selectedShapes.size(); ++i)
                                        shapeContainer.shapes.remove(selectedShapes.get(i));
                                    selectedShapes.clear();
                                }
                                cursorContainer.removeCursorByIndex(cursorIndex);

                                /** Retour au mode Neutre dû au relâchement du bouton 'Effacer' **/
                                if (cursorContainer.getNumCursors() == 0) {
                                    currentMode = MODE_NEUTRAL;
                                    Log.i("OPTION", "Retour au mode Neutre");
                                }
                            }
                            break;

                        /**********************************************************************
                         * RÉSUMER:
                         *
                         * 		Permet de supporter l'ajout du mode Encadrer
                         *
                         **********************************************************************/
                        case MODE_ENCADRER:

                            if (type == MotionEvent.ACTION_UP) {
                                gw.frame(shapeContainer.getBoundingRectangle(), true);
                                cursorContainer.clearCursors();
                                currentMode = MODE_NEUTRAL;
                            }
                            break;

                        /**********************************************************************
                         * RÉSUMER:
                         *
                         * 		Permet de supporter l'ajout du mode Créer.
                         * 	    Pour l'instant, c'est un ajout de forme statique.
                         * 	    Prochainement, on devrait pouvoir ajouter une forme au touché
                         * 	    de l'écran et changer dynamiquement la forme pour ensuite relâcher
                         * 	    et créer la forme.
                         *
                         **********************************************************************/
                        case MODE_CREER:

                            /**  Une forme possède au moins 3 points + 1 point pour le bouton 'Créer' **/
                            if (type == MotionEvent.ACTION_DOWN && cursorContainer.getNumCursors() >= 4) {

                                /**  Ajout d'une forme en fonction du nombre de curseurs **/
                                cursorCreateShape();

                                /** DEBUG: Pour une forme de 4 points ou plus, on retire la dernière forme du conteneur **/
                                if (cursorContainer.getNumCursors() >= 5)
                                    shapeContainer.shapes.remove(shapeContainer.shapes.size() - 1);
                            }
                            else if (type == MotionEvent.ACTION_MOVE && cursorContainer.getNumCursors() >= 4) {

                                /**  Retire la dernière forme du conteneur pour afficher dynamiquement une seule création de forme  **/
                                shapeContainer.shapes.remove(shapeContainer.shapes.size() - 1);

                                /**  Ajout d'une forme en fonction du nombre de curseurs **/
                                cursorCreateShape();
                            }
                            else if (type == MotionEvent.ACTION_UP) {

                                /** On retire les curseurs supplementaires **/
                                cursorContainer.removeCursorByIndex(cursorIndex);
                                for (int i = 0; i < cursorContainer.getNumCursors(); i++)
                                    cursorContainer.removeCursorByIndex(i);

                                /** Retour au mode Neutre en relâchement entièrement l'écran **/
                                if (cursorContainer.getNumCursors() == 0) {
                                    currentMode = MODE_NEUTRAL;
                                    Log.i("OPTION", "Retour au mode Neutre");
                                }
                            }
                            break;
                    }
                    v.invalidate();
                    return true;
                }
            };
        }
        return touchListener;
    }
}