/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.api.model;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.wicket.model.IModel;

/**
 * Lazy loading Wicket model. This is an IModel implementation
 * that loads the model content only when needed. This is supposed
 * to be used for objects that are expensive to load. So we can avoid
 * loading the at all when a component that uses them is not visible.
 * 
 * @author lazyman
 */
public abstract class LoadableModel<T> implements IModel<T> {

    private T object;
    private boolean loaded = false;
    private boolean allwaysReload;

    public LoadableModel() {
        this(null, true);
    }

    public LoadableModel(boolean allwaysReload) {
        this(null, allwaysReload);
    }

    public LoadableModel(T object) {
        this(object, true);
    }

    public LoadableModel(T object, boolean allwaysReload) {
        this.object = object;
        this.allwaysReload = allwaysReload;
    }

    public T getObject() {
        if (!loaded) {
            setObject(load());
            onLoad();
            this.loaded = true;
        }

        if (object instanceof IModel) {
            IModel model = (IModel) object;
            return (T) model.getObject();
        }
        return object;
    }

    public void setObject(T object) {
        if (this.object instanceof IModel) {
            ((IModel<T>) this.object).setObject(object);
        } else {
            this.object = object;
        }

        this.loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void reset() {
        loaded = false;
    }

    public void detach() {
        if (loaded && allwaysReload) {
            this.loaded = false;
            onDetach();
        }
    }

    public IModel getNestedModel() {
        if (object instanceof IModel) {
            return (IModel) object;
        } else {
            return null;
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(":attached=").append(loaded).append(":object=[").append(this.object).append("]");
        return builder.toString();
    }

    protected abstract T load();

    protected void onLoad() {
    }

    protected void onDetach() {
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        if (isLoaded()) {
            WebComponentUtil.reviveObject(object, prismContext);
        }
    }

}