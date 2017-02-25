package uk.org.ngo.squeezer.service;

import android.support.v4.app.Fragment.InstantiationException;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.util.Reflection;

/**
 * Base class that constructs a list of model objects based on CLI results from
 * the server.
 *
 * @param <T> Item subclasses.
 */
abstract class BaseListHandler<T extends Item> implements ListHandler<T> {
    private static final String TAG = BaseListHandler.class.getSimpleName();

    protected List<T> items;

    @SuppressWarnings("unchecked")
    private final Class<T> dataType = (Class<T>) Reflection
            .getGenericClass(getClass(), ListHandler.class, 0);

    /**
     * Cache between classes and the createFromMap method, to avoid repeated lookups.
     */
    private static final HashMap<Class<? extends Item>, Method> methodMap = new HashMap<>(20);

    @Override
    public Class<T> getDataType() {
        return dataType;
    }

    @Override
    public List<T> getItems() {
        return items;
    }

    @Override
    public void clear() {
        items = new ArrayList<T>() {
            private static final long serialVersionUID = 1321113152942485275L;
        };
    }

    @Override
    public void add(Map<String, String> record) {
        Method method = methodMap.get(dataType);
        if (method == null) {
            Method[] methods = dataType.getDeclaredMethods();
            for (Method m : methods) {
                if ("fromMap".equals(m.getName())) {
                    method = m;
                    break;
                }
            }
            methodMap.put(dataType, method);
        }

        try {
            items.add((T)method.invoke(null, record));
        } catch (Exception e) {
            Log.e(TAG, "Unable to create new " + dataType.getName() + ": " + e);
            throw new InstantiationException("Unable to create new " + dataType.getName(), e);
        }
    }
}
