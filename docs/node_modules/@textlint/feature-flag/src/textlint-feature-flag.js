// MIT © 2017 azu
"use strict";
const MapLike = require("map-like").MapLike;
const assert = require("assert");
const flagMap = new MapLike();
/**
 * IT IS FOR TESTING
 */
export const resetFlags = () => {
    flagMap.clear();
};
/**
 * set feature flag
 * @param {string} flagName
 * @param {boolean} status
 */
export const setFeature = (flagName, status) => {
    flagMap.set(flagName, status);
};
/**
 * If the feature flag of `flagName` is enabled, return true.
 * @param {string} flagName
 * @param {boolean=false} [loose]
 * @returns {boolean}
 */
export const isFeatureEnabled = (flagName, { loose = false } = {}) => {
    if (!flagMap.has(flagName)) {
        if (loose) {
            // loose-mode, return false
            return false;
        } else {
            throw new Error(`@textlint/feature-flag:Error: ${flagName} is not defined.`);
        }
    }
    const status = flagMap.get(flagName);
    if (process.env.NODE_ENV !== "production") {
        assert(typeof status, `flag should be boolean, but it is :${status}`);
    }
    return status;
};

// == CORE Flags
// if run textlint --experimental, set experimental true by default
if (process && Array.isArray(process.argv) && process.argv.indexOf("--experimental") !== -1) {
    setFeature("core.experimental", true);
}
/**
 * Core flags
 * @type {{experimental, experimental, runningCLI, runningCLI, runningTester, runningTester}}
 */
export const coreFlags = {
    // Experimental
    get experimental() {
        return isFeatureEnabled("core.experimental", {
            loose: true
        });
    },
    set experimental(status) {
        setFeature("core.experimental", status);
    },
    // CLI
    get runningCLI() {
        return isFeatureEnabled("core.runningCLI", {
            loose: true
        });
    },
    set runningCLI(status) {
        setFeature("core.runningCLI", status);
    },
    // textlint-tester
    get runningTester() {
        return isFeatureEnabled("core.runningTester", {
            loose: true
        });
    },
    set runningTester(status) {
        setFeature("core.runningTester", status);
    }
};
/**
 * if current is not experimental, throw error message.
 * @param message
 */
export function throwWithoutExperimental(message) {
    if (coreFlags.runningCLI && !coreFlags.experimental) {
        throw Error(message);
    }
}
/**
 * if current is in testing, throw error message.
 * @param {string} message
 */
export function throwIfTesting(message) {
    if (coreFlags.runningTester) {
        throw Error(message);
    }
}
