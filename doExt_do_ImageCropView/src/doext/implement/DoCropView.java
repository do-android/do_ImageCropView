package doext.implement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class DoCropView extends View {
	private Paint paint = new Paint();
	private Paint borderPaint = new Paint();

	/** 自定义顶部栏高度，如不是自定义，则默认为0即可 */
	private int customTopBarHeight = 0;
	/** 裁剪框长宽比，默认4：3 */
	private double cropRatio = 0.75;
	/** 裁剪框宽度 */
	private int cropWidth = -1;
	/** 裁剪框高度 */
	private int cropHeight = -1;
	/** 裁剪框左边空留宽度 */
	private int cropLeftMargin = 0;
	/** 裁剪框上边空留宽度 */
	private int cropTopMargin = 0;
	/** 裁剪框边框宽度 */
	private int cropBorderWidth = 1;
	private OnDrawListenerComplete listenerComplete;
	private boolean isSetMargin = false;

	public DoCropView(Context context) {
		super(context);
	}

	public DoCropView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DoCropView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int width = this.getWidth();
		int height = this.getHeight();
		// 如没有显示设置裁剪框高度和宽度，取默认值
		if (cropWidth == -1 || cropHeight == -1) {
			cropWidth = width - 50;
			cropHeight = (int) (cropWidth * cropRatio);
			// 横屏
			if (width > height) {
				cropHeight = height - 50;
				cropWidth = (int) (cropHeight / cropRatio);
			}
		}
		// 如没有显示设置裁剪框左和上预留宽度，取默认值
		if (!isSetMargin) {
			cropLeftMargin = (width - cropWidth) / 2;
			cropTopMargin = (height - cropHeight) / 2;
		}
		// 画阴影
		paint.setAlpha(200);
		// top
		canvas.drawRect(0, customTopBarHeight, width, cropTopMargin, paint);
		// left
		canvas.drawRect(0, cropTopMargin, cropLeftMargin, cropTopMargin + cropHeight, paint);
		// right
		canvas.drawRect(cropLeftMargin + cropWidth, cropTopMargin, width, cropTopMargin + cropHeight, paint);
		// bottom
		canvas.drawRect(0, cropTopMargin + cropHeight, width, height, paint);

		// 画边框
		borderPaint.setStyle(Style.STROKE);
		borderPaint.setColor(Color.WHITE);
		borderPaint.setStrokeWidth(cropBorderWidth);
		canvas.drawRect(cropLeftMargin, cropTopMargin, cropLeftMargin + cropWidth, cropTopMargin + cropHeight, borderPaint);

		if (listenerComplete != null) {
			listenerComplete.onDrawCompelete();
		}
	}

	public int getCustomTopBarHeight() {
		return customTopBarHeight;
	}

	public void setCustomTopBarHeight(int customTopBarHeight) {
		this.customTopBarHeight = customTopBarHeight;
	}

	public double getCropRatio() {
		return cropRatio;
	}

	public void setCropRatio(double cropRatio) {
		this.cropRatio = cropRatio;
	}

	public int getCropWidth() {
		// 减cropBorderWidth原因：截图时去除边框白线
		return cropWidth - cropBorderWidth;
	}

	public void setCropWidth(int cropWidth) {
		this.cropWidth = cropWidth;
	}

	public int getCropHeight() {
		return cropHeight - cropBorderWidth;
	}

	public void setCropHeight(int cropHeight) {
		this.cropHeight = cropHeight;
	}

	public int getCropLeftMargin() {
		return cropLeftMargin + cropBorderWidth;
	}

	public void setCropLeftMargin(int cropLeftMargin) {
		this.cropLeftMargin = cropLeftMargin;
		isSetMargin = true;
	}

	public int getCropTopMargin() {
		return cropTopMargin + cropBorderWidth;
	}

	public void setCropTopMargin(int cropTopMargin) {
		this.cropTopMargin = cropTopMargin;
		isSetMargin = true;
	}

	public void addOnDrawCompleteListener(OnDrawListenerComplete listener) {
		this.listenerComplete = listener;
	}

	public void removeOnDrawCompleteListener() {
		this.listenerComplete = null;
	}

	/**
	 * 裁剪区域画完时调用接口
	 * 
	 * @author Cow
	 * 
	 */
	public interface OnDrawListenerComplete {
		public void onDrawCompelete();
	}
}
