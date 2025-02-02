/*
 * Contributions to SpotBugs
 * Copyright (C) 2021 Mikael Ståldal
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package ghIssues;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1397">GitHub issue</a>
 */
public class Issue1397 {
    CompletionStage<Void> completedFuture() {
        // 1st argument of CompletableFuture#completedStage(U) should be nullable
        return CompletableFuture.completedStage(null);
    }
}
