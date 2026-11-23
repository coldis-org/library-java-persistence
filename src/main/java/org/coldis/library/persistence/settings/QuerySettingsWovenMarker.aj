package org.coldis.library.persistence.settings;

/**
 * Marks every QuerySettings-annotated class and method the weaver reaches with QuerySettingsWoven,
 * so the start-up check can tell a woven bean from one compiled without this library's aspects
 * (whose annotation would otherwise be ignored silently). Code style, because annotation-style
 * aspects cannot declare annotations.
 */
public aspect QuerySettingsWovenMarker {

	declare @method : @QuerySettings * *(..) : @QuerySettingsWoven;

	declare @type : @QuerySettings * : @QuerySettingsWoven;

}
