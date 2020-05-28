package project;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

/**
 * @author Alexandre Laroche
 */
public class Application extends Activity {
  DrawingView drawView;

  /* Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //Setting application for fullscreen mode
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    //Setting the view
    drawView = new DrawingView(this);
    setContentView(drawView);

    drawView.requestFocus();
  }
}

