package doext.implement;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoImageLoadHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoUIModule;
import doext.define.do_ImageCropView_IMethod;
import doext.define.do_ImageCropView_MAbstract;
import doext.implement.DoCropView.OnDrawListenerComplete;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,do_ImageCropView_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_ImageCropView_View extends FrameLayout implements DoIUIModuleView, do_ImageCropView_IMethod, OnTouchListener {

	private ImageView srcPic;
	private DoCropView cropView;
	private Bitmap bitmap;

	private int width;
	private int height;

	private double xZoom;
	private double yZoom;

	private int cropDefaultWidth;
	private int cropDefaultHeight;

	private Activity mContext;

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_ImageCropView_MAbstract model;

	public do_ImageCropView_View(Context context) {
		super(context);
		this.mContext = (Activity) context;
		srcPic = new ImageView(context);
		this.addView(srcPic, new LayoutParams(-1, -1));
		srcPic.setOnTouchListener(this);
		cropView = new DoCropView(mContext);
	}

	/**
	 * 初始化截图区域，并将源图按裁剪框比例缩放
	 * 
	 * @param _top
	 */
	private void initClipView(int _top) {
		cropView.setCustomTopBarHeight(_top);
		cropView.addOnDrawCompleteListener(new OnDrawListenerComplete() {

			public void onDrawCompelete() {
				cropView.removeOnDrawCompleteListener();
				int _cropWidth = cropView.getCropWidth();
				int _cropHeight = cropView.getCropHeight();
				int _midX = cropView.getCropLeftMargin() + (_cropWidth / 2);
				int _midY = cropView.getCropTopMargin() + (_cropHeight / 2);

				int _imageWidth = bitmap.getWidth();
				int _imageHeight = bitmap.getHeight();
				// 按裁剪框求缩放比例
				float _scale = (_cropWidth * 1.0f) / _imageWidth;
				if (_imageWidth > _imageHeight) {
					_scale = (_cropHeight * 1.0f) / _imageHeight;
				}

				// 起始中心点
				float _imageMidX = _imageWidth * _scale / 2;
				float _imageMidY = cropView.getCustomTopBarHeight() + _imageHeight * _scale / 2;
				srcPic.setScaleType(ScaleType.MATRIX);

				// 缩放
				matrix.postScale(_scale, _scale);
				// 平移
				matrix.postTranslate(_midX - _imageMidX, _midY - _imageMidY);

				srcPic.setImageMatrix(matrix);
				srcPic.setImageBitmap(bitmap);
			}
		});
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_ImageCropView_MAbstract) _doUIModule;
		width = (int) _doUIModule.getWidth();
		height = (int) _doUIModule.getHeight();

		xZoom = _doUIModule.getXZoom();
		yZoom = _doUIModule.getYZoom();

		cropDefaultWidth = width / 2;
		cropDefaultHeight = height / 2;

		cropView.setCropWidth((int) (cropDefaultWidth * xZoom));
		cropView.setCropHeight((int) (cropDefaultHeight * yZoom));
		this.addView(cropView, new LayoutParams(-1, -1));
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("source")) {
			String source = _changedValues.get("source");
			try {
				if (source != null && !"".equals(source)) {
					String path = DoIOHelper.getLocalFileFullPath(this.model.getCurrentPage().getCurrentApp(), source);
					bitmap = DoImageLoadHelper.getInstance().loadLocal(path, (int) model.getWidth(), (int) model.getHeight());
					if (bitmap != null) {
						srcPic.setImageBitmap(bitmap);
						initClipView(srcPic.getTop());
						matrix.reset();
						cropView.postInvalidate();
					}
				}
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("do_ImageCropView_View source \n\t", e);
			}
		}

		if (_changedValues.containsKey("cropArea")) {
			String _cropArea = _changedValues.get("cropArea");
			if (null != _cropArea && !"".equals(_cropArea)) {
				String[] _area = _cropArea.split(",");
				if (null != _area && _area.length == 2) {

					int _width = DoTextHelper.strToInt(_area[0], cropDefaultWidth);
					int _height = DoTextHelper.strToInt(_area[1], cropDefaultHeight);

					if (_width > width) {
						_width = width;
					}

					if (_height > height) {
						_height = height;
					}

					cropView.setCropWidth((int) (_width * xZoom));
					cropView.setCropHeight((int) (_height * yZoom));
					cropView.postInvalidate();
				}
			}
		}

	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @throws Exception
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("crop".equals(_methodName)) { // 执行动画
			this.crop(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}

		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 裁剪图片；
	 * 
	 * @throws Exception
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void crop(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if (bitmap == null) {
			throw new Exception("source属性不能为空！");
		}

		// 获取截屏
		View view = mContext.getWindow().getDecorView();
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();

		int[] location = new int[2];
		this.getLocationOnScreen(location);

		int _x = cropView.getCropLeftMargin() + location[0];
		int _y = cropView.getCropTopMargin() + location[1];
		int _width = cropView.getCropWidth();
		int _height = cropView.getCropHeight();

		Bitmap bmp = Bitmap.createBitmap(view.getDrawingCache(), _x, _y, _width, _height);
		// 释放资源
		view.destroyDrawingCache();

		ByteArrayOutputStream _photoData = new ByteArrayOutputStream();
		if (bmp != null) {
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, _photoData);
		}
		String _fileName = DoTextHelper.getTimestampStr() + ".png.do";
		String _fileFullName = _scriptEngine.getCurrentApp().getDataFS().getRootPath() + "/temp/do_ImageCropView/" + _fileName;

		DoIOHelper.writeAllBytes(_fileFullName, _photoData.toByteArray());
		String _url = "data://temp/do_ImageCropView/" + _fileName;

		DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
		_invokeResult.setResultText(_url);
		_scriptEngine.callback(_callbackFuncName, _invokeResult);

	}

	// ////////////////////////imageview可以支持拖动和缩放
	private Matrix matrix = new Matrix();
	private Matrix savedMatrix = new Matrix();

	/** 动作标志：无 */
	private static final int NONE = 0;
	/** 动作标志：拖动 */
	private static final int DRAG = 1;
	/** 动作标志：缩放 */
	private static final int ZOOM = 2;
	/** 初始化动作标志 */
	private int mode = NONE;

	/** 记录起始坐标 */
	private PointF start = new PointF();
	/** 记录缩放时两指中间点坐标 */
	private PointF mid = new PointF();
	private float oldDist = 1f;

	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouch(View v, MotionEvent event) {
		ImageView view = (ImageView) v;
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			savedMatrix.set(matrix);
			// 设置开始点位置
			start.set(event.getX(), event.getY());
			mode = DRAG;
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = spacing(event);
			if (oldDist > 10f) {
				savedMatrix.set(matrix);
				midPoint(mid, event);
				mode = ZOOM;
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG) {
				matrix.set(savedMatrix);
				matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
			} else if (mode == ZOOM) {
				float newDist = spacing(event);
				if (newDist > 10f) {
					matrix.set(savedMatrix);
					float scale = newDist / oldDist;
					matrix.postScale(scale, scale, mid.x, mid.y);
				}
			}
			break;
		}
		view.setImageMatrix(matrix);
		return true;
	}

	/**
	 * 多点触控时，计算最先放下的两指距离
	 * 
	 * @param event
	 * @return
	 */
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	/**
	 * 多点触控时，计算最先放下的两指中心坐标
	 * 
	 * @param point
	 * @param event
	 */
	private void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}
	// ///////////////////////////////
}