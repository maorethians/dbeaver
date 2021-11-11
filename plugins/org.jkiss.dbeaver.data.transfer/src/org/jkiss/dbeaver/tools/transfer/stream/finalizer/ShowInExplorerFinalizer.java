/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.stream.finalizer;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamTransferFinalizer;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamTransferFinalizerConfigurator;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;

import java.io.File;

public class ShowInExplorerFinalizer implements IStreamTransferFinalizer, IStreamTransferFinalizerConfigurator {
    @Override
    public void finish(@NotNull DBRProgressMonitor monitor, @NotNull StreamTransferConsumer consumer, @NotNull StreamConsumerSettings settings) throws DBException {
        // TODO: Figure a way to visualize availability in UI (dynamically disable checkbox if output is clipboard)
        // TODO: We can use finalizers for other things as well, but it will require a way to construct UI.

        if (!settings.isOutputClipboard()) {
            final String folder = consumer.getOutputFolder();
            final String filename = consumer.getOutputFileName();
            DBWorkbench.getPlatformUI().showInSystemExplorer(new File(folder, filename).getAbsolutePath());
        }
    }

    @Override
    public boolean isApplicable(@NotNull StreamConsumerSettings settings) {
        return !settings.isOutputClipboard();
    }
}
