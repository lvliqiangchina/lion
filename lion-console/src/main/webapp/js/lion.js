/**************************全局Ajax返回码*********************************/
var Res_Code_Success = 0;
var Res_Code_Error = -1;
var Res_Code_Warn = 1;
/**************************对String对象的扩展*********************************/
/**
 * 在字符串前拼接contextpath 
 */
String.prototype.prependcontext = function() {
	if (typeof contextpath == "undefined") {
		throw new Error("global variable contextpath not found, maybe you not include common.ftl.");
	}
	var result = (this.startsWith("/") ? "" : "/") + this;
	return result.startsWith(contextpath) ? result: contextpath + result;
}

/**
 * 替换字符串中指定的字符串
 */
String.prototype.replaceAll = function(reallyDo, replaceWith, ignoreCase) {  
	if (!RegExp.prototype.isPrototypeOf(reallyDo)) {  
		return this.replace(new RegExp(reallyDo, (ignoreCase ? "gi": "g")), replaceWith);  
	} else {  
		return this.replace(reallyDo, replaceWith);  
	}  
}

String.prototype.escapeQuotes = function() {
	if (this == void 0) {throw new Error("Illegal argument error.");}
	return this.replaceAll("\"", "&#34;").replaceAll("'", "&#39;");
}

/**
* 去除两端空格
*/
String.prototype.trim = function() {
	if (this == void 0) {throw new Error("Illegal argument error.");}
	return this.replace(/(^\s+)|(\s+$)/g, "");
}

String.prototype.trimIf = function(ifTrim) {
	if (this == void 0) {throw new Error("Illegal argument error.");}
	return ifTrim == true ? this.trim() : this.toString();
}

/**
 * 判断字符串是否为null or empty string
 */
String.prototype.isEmpty = function() {
	if (this == void 0) {throw new Error("Illegal argument error.");}
	return this == null || this.length == 0;
}

/**
 * 判断字符串是否为null or blank string 
 */
String.prototype.isBlank = function() {
	if (this == void 0) {throw new Error("Illegal argument error.");}
	return this == null || this.trim().length == 0;
}

/**
* 判断字符串是否以指定的字符串开始
*/
String.prototype.startsWith = function(str) {
	if (this == void 0) {throw new Error("Illegal argument error.");}
    return this.substr(0, str.length) == str;
}

String.prototype.isNumber = function() {
	if (this == void 0) {throw new Error("Illegal argument error.");}
	return this != null && /^-?(?:\d+|\d{1,3}(?:,\d{3})+)(?:\.\d+)?$/.test(this);
}

String.prototype.left = function(maxSize) {
	if (this == void 0) {throw new Error("Illegal argument error.");}
	return this.slice(0, maxSize - this.slice(0, maxSize).replace(/[\x00-\xff]/g,"").length);
}

String.prototype.abbreviate = function(maxSize) {
	if (this == void 0) {throw new Error("Illegal argument error.");}
	return this.chineseLength() > maxSize ? this.left(maxSize) + "..." : this.toString();
}

String.prototype.chineseLength = function() {
	return this.replace(/[^\x00-\xff]/g,"**").length;
}

String.prototype.substringAfterLast = function(separator) {
	if (this.isEmpty()) {
		return this;
	}
	if (separator.isEmpty()) {
		return String.EMPTY;
	}
	var pos = this.lastIndexOf(separator);
	if (pos == -1 || pos == (this.length - separator.length)) {
		return String.EMPTY;
	}
	return this.substring(pos + separator.length);
}

/**************************对Array对象的扩展*********************************/
Array.prototype.contains = function(obj) {
	for (var i = 0; i < this.length; i++) {
		if (this[i] === obj) {
			return true;
		}
	}
	return false;
}

/************************用js实现的一个Map***********************/

function Map(){
	this.container = new Object();
}

Map.prototype.put = function(key, value){
	this.container[key] = value;
}

Map.prototype.get = function(key){
	return this.container[key];
}

Map.prototype.contains = function(key) {
	return this.get(key) != undefined;
}

Map.prototype.keySet = function() {
	var keyset = new Array();
	var count = 0;
	for (var key in this.container) {
		// 跳过object的extend函数
		if (key == 'extend')
			continue;
			
		keyset[count] = key;
		count++;
	}
	
	return keyset;
}

Map.prototype.size = function() {
	var count = 0;
	for (var key in this.container) {
		// 跳过object的extend函数
		if (key == 'extend')
			continue;
			
		count++;
	}
	
	return count;
}

Map.prototype.toString = function(){
	var str = "";
	for (var i = 0, keys = this.keySet(), len = keys.length; i < len; i++) {
		str = str + keys[i] + "=" + this.container[keys[i]] + ";\n";
	}
	return str;
}

/**************************对JQuery对象的扩展*********************************/
$.fn.extend({
	flashAlert : function(msg, flashInterval) {
		this.html(msg).parents(".alert-error,.alert-warn,.alert-success").stop().show("fast", function() {
			$(this).delay($.type(flashInterval) === "undefined" ? 2000: flashInterval).fadeOut("slow");
		});
	},
	showAlert : function(msg) {
		this.html(msg).parents(".alert-error,.alert-warn,.alert-success").show();
	},
	hideAlert : function() {
		this.html("").parents(".alert-error,.alert-warn,.alert-success").hide();
	},
	hideAlerts : function() {
		this.find(".alert-error,.alert-warn,.alert-success").hide();
	}
});

$.extend({
	isJSonStrList : function(value) {
		value = value.trim();
		if (value.isEmpty()) {
			return true;
		}
		if (!value.startsWith("[")) {
			return false;
		}
		try {
			var json = $.parseJSON(value);
			for (var i = 0; i < json.length; i++) {
				if (typeof json[i] != "string") {
					return false;
				}
			}
			return true;
		} catch (err) {
			return false;
		}
	},
	isJSonNumList : function(value) {
		value = value.trim();
		if (value.isEmpty()) {
			return true;
		}
		if (!value.startsWith("[")) {
			return false;
		}
		try {
			var json = $.parseJSON(value);
			for (var i = 0; i < json.length; i++) {
				if (!json[i].isNumber()) {
					return false;
				}
			}
			return true;
		} catch (err) {
			return false;
		}
	},
	isJSonObj : function(value) {
		value = value.trim();
		if (value.isEmpty()) {
			return true;
		}
		if (!value.startsWith("{")) {
			return false;
		}
		try {
			$.parseJSON(value);
			return true;
		} catch (err) {
			return false;
		}
	}
});

if ($.browser.msie) {
	$(location).attr("href", "/noie.ftl".prependcontext());
}
