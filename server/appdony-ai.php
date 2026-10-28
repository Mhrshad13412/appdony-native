<?php
/**
 * Plugin Name: Appdony AI Proxy
 */

add_action('rest_api_init', function () {
    register_rest_route('appdony/v1', '/ai', [
        'methods'             => 'POST',
        'callback'            => 'appdony_ai_proxy',
        'permission_callback' => '__return_true',
    ]);
});

function appdony_ai_proxy(WP_REST_Request $request) {
    $key = defined('APPDONY_LLM7_KEY') ? APPDONY_LLM7_KEY : '';
    if (!$key) {
        return new WP_REST_Response(['error' => 'not configured'], 500);
    }

    $params   = $request->get_json_params();
    $messages = (isset($params['messages']) && is_array($params['messages'])) ? $params['messages'] : [];
    if (!$messages) {
        return new WP_REST_Response(['error' => 'no messages'], 400);
    }

    // محدودیت ساده: ۳۰ پیام در دقیقه برای هر IP
    $ip    = isset($_SERVER['REMOTE_ADDR']) ? $_SERVER['REMOTE_ADDR'] : 'x';
    $rlKey = 'appdony_ai_' . md5($ip);
    $count = (int) get_transient($rlKey);
    if ($count >= 30) {
        return new WP_REST_Response(['error' => 'rate limit'], 429);
    }
    set_transient($rlKey, $count + 1, MINUTE_IN_SECONDS);

    $response = wp_remote_post('https://api.llm7.io/v1/chat/completions', [
        'timeout' => 60,
        'headers' => [
            'Authorization' => 'Bearer ' . $key,
            'Content-Type'  => 'application/json',
        ],
        'body'    => wp_json_encode([
            'model'       => 'fast',
            'messages'    => array_slice($messages, -20),
            'temperature' => 0.7,
        ]),
    ]);

    if (is_wp_error($response)) {
        return new WP_REST_Response(['error' => $response->get_error_message()], 502);
    }

    $code = wp_remote_retrieve_response_code($response);
    $body = json_decode(wp_remote_retrieve_body($response), true);
    $text = isset($body['choices'][0]['message']['content']) ? $body['choices'][0]['message']['content'] : '';

    if ($code !== 200 || $text === '') {
        return new WP_REST_Response(['error' => 'upstream ' . $code], 502);
    }

    return new WP_REST_Response(['reply' => $text], 200);
}
