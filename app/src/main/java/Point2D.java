//
//package log350.example.example6;
//
//import java.util.ArrayList;
//import java.util.Timer;
//
////import java.util.List;
//
//import android.content.Context;
////import android.graphics.Matrix;
//import android.graphics.Canvas;
////import android.graphics.Rect;
////import android.graphics.Path;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.util.Log;
//import android.view.MotionEvent;
//import android.view.View;
//
//
//// This class stores the current position of a finger,
//// as well as the history of previous positions of that finger
//// during its drag.
////
//// An instance of this class is created when a finger makes contact
//// with the multitouch surface.  The instance stores all
//// subsequent positions of the finger, and is destroyed
//// when the finger is lifted off the multitouch surface.
//
///*****************************************************************************
// *
// *                      CLASS: MyCursor
// *
// *****************************************************************************/
//class MyCursor {
//
//	// Each finger in contact with the multitouch surface is given
//	// a unique id by the framework (or computing platform).
//	// There is no guarantee that these ids will be consecutive nor increasing.
//	// For example, when two fingers are in contact with the multitouch surface,
//	// their ids may be 0 and 1, respectively,
//	// or their ids may be 14 and 9, respectively.
//	public int id; // identifier
//
//	// This stores the history of positions of the "cursor" (finger)
//	// in pixel coordinates.
//	// The first position is

package log350.example.example6;


public class Point2D {

	public float [] p = new float[2];

	public Point2D() {
		p[0] = p[1] = 0;
	}

	public Point2D( float x, float y ) {
		p[0] = x;
		p[1] = y;
	}

	public Point2D( Point2D P ) {
		p[0] = P.p[0];
		p[1] = P.p[1];
	}

	public Point2D( Vector2D V ) {
		p[0] = V.v[0];
		p[1] = V.v[1];
	}

	public void copy( float x, float y ) {
		p[0] = x;
		p[1] = y;
	}

	public void copy( Point2D P ) {
		p[0] = P.p[0];
		p[1] = P.p[1];
	}

	public boolean equals( Point2D other ) {
		return x() == other.x() && y() == other.y();
	}

	public float x() { return p[0]; }
	public float y() { return p[1]; }

	// used to pass coordinates directly to OpenGL routines
	public float [] get() { return p; }

	// return the difference between two given points
	static public Vector2D diff( Point2D a, Point2D b ) {
		return new Vector2D( a.x()-b.x(), a.y()-b.y() );
	}

	// return the sum of the given point and vector
	static public Point2D sum( Point2D a, Vector2D b ) {
		return new Point2D( a.x()+b.x(), a.y()+b.y() );
	}

	// return the difference between the given point and vector
	static public Point2D diff( Point2D a, Vector2D b ) {
		return new Point2D( a.x()-b.x(), a.y()-b.y() );
	}

	public float distance( Point2D otherPoint ) {
		return diff( this, otherPoint ).length();
	}

	static Point2D average( Point2D a, Point2D b ) {
		// return new Point2D( Vector2D.mult( Vector2D.sum( new Vector2D(a), new Vector2D(b) ), 0.5f ) );
		return new Point2D( (a.x()+b.x())*0.5f, (a.y()+b.y())*0.5f );
	}

}

