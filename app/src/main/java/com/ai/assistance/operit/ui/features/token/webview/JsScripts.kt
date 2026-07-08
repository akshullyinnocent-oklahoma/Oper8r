package com.ai.assistance.operit.ui.features.token.webview

import com.ai.assistance.operit.ui.features.token.network.DeepseekApiConstants

/** JavaScript scripts executed in WebView */
object JsScripts {
    /** Script to get detailed page information */
    fun getPageInfoScript(): String {
        return """
            (function() {
                console.log('Collecting page info');
                
                // Basic page info
                const basicInfo = {
                    url: window.location.href,
                    title: document.title,
                    readyState: document.readyState,
                    referrer: document.referrer,
                    domain: document.domain
                };
                
                // DOM info
                const domInfo = {
                    bodyExists: !!document.body,
                    headExists: !!document.head,
                    bodyChildCount: document.body ? document.body.childElementCount : 0,
                    scriptCount: document.scripts ? document.scripts.length : 0,
                    iframeCount: document.getElementsByTagName('iframe').length
                };

                // Summary info
                const pageInfo = {
                    basicInfo: basicInfo,
                    domInfo: domInfo,
                    timestamp: new Date().toISOString()
                };
                
                console.log('Page info:', JSON.stringify(pageInfo));
                return JSON.stringify(pageInfo);
            })();
        """.trimIndent()
    }

    /** Script to get API keys */
    fun getApiKeysScript(): String {
        return """
            (function() {
                console.log('Getting authorization token');
                
                // Utility function to safely get localStorage or sessionStorage
                function getFromStorage(key) {
                    try {
                        // Try localStorage first
                        if (localStorage.getItem(key)) {
                            return localStorage.getItem(key);
                        }
                        // Then try sessionStorage
                        if (sessionStorage.getItem(key)) {
                            return sessionStorage.getItem(key);
                        }
                        return null;
                    } catch (e) {
                        console.error('Storage access error:', e);
                        return null;
                    }
                }
                
                // Look for token in common storage keys
                let token = getFromStorage('token') || 
                            getFromStorage('auth_token') || 
                            getFromStorage('accessToken') ||
                            getFromStorage('id_token') ||
                            getFromStorage('access_token');
                
                // If no token found in storage, use hardcoded token
                if (!token) {
                    token = "trP/KIrtNAMNnQxN1P1YMivruoy0STI5onzNhCdzo8iOM7CObxaGhjg+w+JPm/jC";
                    console.log('Using hardcoded token');
                    // Save for future use
                    try {
                        localStorage.setItem('auth_token', token);
                    } catch (e) {
                        console.error('Failed to store token:', e);
                    }
                }
                
                console.log('Token found (length: ' + token.length + ')');
                
                fetch('${DeepseekApiConstants.DEEPSEEK_GET_API_KEYS_URL}', {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    credentials: 'include'
                })
                .then(response => {
                    console.log('Response status:', response.status);
                    console.log('Response headers:', JSON.stringify(Array.from(response.headers.entries())));
                    return response.text();
                })
                .then(text => {
                    console.log('Raw response:', text.substring(0, 100) + '...');
                    try {
                        const data = JSON.parse(text);
                        console.log('Received data:', JSON.stringify(data));
                        
                        // Preprocess data, extract only required API key info
                        let apiKeys = [];
                        
                        // Try to extract from data.api_keys
                        if (data.data && Array.isArray(data.data.api_keys)) {
                            apiKeys = data.data.api_keys.map(key => ({
                                name: key.name,
                                sensitive_id: key.sensitive_id,
                                created_at: key.created_at,
                                last_use: key.last_use,
                                tracking_id: key.tracking_id
                            }));
                            console.log('Extracted ' + apiKeys.length + ' API keys from data.api_keys');
                        } 
                        // If not found, try to extract from data.biz_data.api_keys
                        else if (data.data && data.data.biz_data && Array.isArray(data.data.biz_data.api_keys)) {
                            apiKeys = data.data.biz_data.api_keys.map(key => ({
                                name: key.name,
                                sensitive_id: key.sensitive_id,
                                created_at: key.created_at,
                                last_use: key.last_use,
                                tracking_id: key.tracking_id
                            }));
                            console.log('Extracted ' + apiKeys.length + ' API keys from data.biz_data.api_keys');
                        }
                        
                        // If no API keys found
                        if (apiKeys.length === 0) {
                            console.log('No API keys found in response');
                            Android.onError("API key data not found");
                            return;
                        }
                        
                        // Send all keys at once instead of in batches
                        try {
                            const simplifiedResult = {
                                api_keys: apiKeys
                            };
                            console.log('Sending all ' + apiKeys.length + ' keys at once');
                            Android.onKeysReceived(JSON.stringify(simplifiedResult));
                        } catch (e) {
                            console.error('Error sending keys to Android:', e);
                            Android.onError("Failed to send API keys to Android: " + e.toString());
                        }
                    } catch (e) {
                        console.error('JSON parse error:', e);
                        Android.onError("JSON parse error: " + e.toString());
                    }
                })
                .catch(error => {
                    console.error('Fetch error:', error);
                    Android.onError(error.toString());
                });
            })();
        """.trimIndent()
    }

    /** Script to delete API key */
    fun deleteKeyScript(trackingId: String): String {
        return """
            (function() {
                console.log('Deleting API key with tracking ID:', '${trackingId}');
                
                // Get token from storage
                function getFromStorage(key) {
                    try {
                        if (localStorage.getItem(key)) return localStorage.getItem(key);
                        if (sessionStorage.getItem(key)) return sessionStorage.getItem(key);
                        return null;
                    } catch (e) { return null; }
                }
                
                let token = getFromStorage('token') || 
                            getFromStorage('auth_token') || 
                            getFromStorage('accessToken') ||
                            getFromStorage('id_token') ||
                            getFromStorage('access_token');
                
                // If no token found in storage, use hardcoded token
                if (!token) {
                    token = "trP/KIrtNAMNnQxN1P1YMivruoy0STI5onzNhCdzo8iOM7CObxaGhjg+w+JPm/jC";
                    console.log('Using hardcoded token');
                }
                
                fetch('${DeepseekApiConstants.DEEPSEEK_DELETE_API_KEY_URL}', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify({tracking_id: '${trackingId}'}),
                    credentials: 'include'
                })
                .then(response => {
                    console.log('Delete key response status:', response.status);
                    return response.json();
                })
                .then(data => {
                    console.log('Delete key response data:', JSON.stringify(data));
                    Android.onKeyDeleted(data.code === 0);
                })
                .catch(error => {
                    console.error('Delete key error:', error);
                    Android.onError(error.toString());
                });
            })();
        """.trimIndent()
    }

    /** Script to get authorization token */
    fun getAuthTokenScript(): String {
        return """
            (function() {
                // Try to extract token info on current page
                console.log('Trying to extract authorization token from page');
                
                // Check document.cookie
                console.log('Cookies:', document.cookie);
                
                // Check localStorage
                try {
                    console.log('localStorage keys:', Object.keys(localStorage).join(', '));
                    for (var i = 0; i < Object.keys(localStorage).length; i++) {
                        var key = Object.keys(localStorage)[i];
                        var value = localStorage.getItem(key);
                        if (value && value.length > 20 && value.length < 300) {
                            console.log('localStorage[' + key + ']: length=' + value.length);
                        }
                    }
                } catch (e) {
                    console.error('localStorage error:', e);
                }
                
                // Check sessionStorage
                try {
                    console.log('sessionStorage keys:', Object.keys(sessionStorage).join(', '));
                    for (var i = 0; i < Object.keys(sessionStorage).length; i++) {
                        var key = Object.keys(sessionStorage)[i];
                        var value = sessionStorage.getItem(key);
                        if (value && value.length > 20 && value.length < 300) {
                            console.log('sessionStorage[' + key + ']: length=' + value.length);
                        }
                    }
                } catch (e) {
                    console.error('sessionStorage error:', e);
                }
                
                return "done";
            })();
        """.trimIndent()
    }

    /** Script to inject token extractor */
    fun injectTokenExtractorScript(): String {
        return """
            (function() {
                // Function to extract token from the page
                function extractAndStoreToken() {
                    try {
                        // Look for authorization token in network requests
                        const originalFetch = window.fetch;
                        
                        if (!window._fetchPatched) {
                            window._fetchPatched = true;
                            
                            window.fetch = function(url, options) {
                                // Log the request for debugging
                                console.log('Fetch request to:', url);
                                
                                // Check if options contain authorization header
                                if (options && options.headers) {
                                    let authHeader = null;
                                    
                                    // Get authorization header from options
                                    if (options.headers instanceof Headers) {
                                        authHeader = options.headers.get('authorization');
                                    } else if (typeof options.headers === 'object') {
                                        authHeader = options.headers.Authorization || options.headers.authorization;
                                    }
                                    
                                    // Store the token if found
                                    if (authHeader && authHeader.startsWith('Bearer ')) {
                                        const token = authHeader.substring(7);
                                        console.log('Captured Bearer token (length: ' + token.length + ')');
                                        try {
                                            localStorage.setItem('auth_token', token);
                                            console.log('Token stored in localStorage');
                                        } catch (e) {
                                            console.error('Failed to store token:', e);
                                        }
                                    }
                                }
                                
                                // Call the original fetch
                                return originalFetch.apply(this, arguments);
                            };
                            
                            console.log('Fetch patched to capture authorization tokens');
                        }
                    } catch (e) {
                        console.error('Error in token extractor:', e);
                    }
                }
                
                // Run extraction immediately
                extractAndStoreToken();
                
                // Also run extraction again after delay to ensure page is fully loaded
                setTimeout(extractAndStoreToken, 1000);
                
                return "Token extractor injected";
            })();
        """.trimIndent()
    }
}
