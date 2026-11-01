#!/usr/bin/env bash
# اجرا داخل امولاتور: نصب، اجرا و جمع‌آوری لاگ. هر مرحله تایم‌اوت داره تا هیچ‌وقت گیر نکنه
# و حتی اگه اپ کرش کرد یا یه مرحله خطا داد، لاگ‌ها ذخیره بشن.
set +e
OUT=crash-logs
mkdir -p "$OUT"
PKG=com.appdony.app
APK=apk/app-release-emu.apk

adb logcat -c
# لاگ زنده از همون ابتدا (حتی اگه اپ وسط کار بمیره، همه‌چیز ثبت شده)
adb logcat -v threadtime > "$OUT/logcat_live.txt" 2>&1 &
LOGPID=$!

echo "== install" | tee "$OUT/steps.txt"
timeout 180 adb install -r "$APK" >> "$OUT/steps.txt" 2>&1
echo "install exit=$?" >> "$OUT/steps.txt"

echo "== launch" >> "$OUT/steps.txt"
timeout 90 adb shell am start -W -n $PKG/.MainActivity >> "$OUT/steps.txt" 2>&1
echo "launch exit=$?" >> "$OUT/steps.txt"

# چند دور چک می‌کنیم؛ اگه اپ زود کرش کرد، منتظر کل زمان نمی‌مونیم
for i in $(seq 1 12); do
  sleep 5
  if [ -z "$(adb shell pidof $PKG | tr -d '\r')" ]; then
    echo "app process is gone after $((i*5))s" >> "$OUT/steps.txt"
    break
  fi
done

# چند ضربه‌ی تصادفی تا صفحه‌ها و لیست‌ها هم تست بشن
timeout 60 adb shell monkey -p $PKG --throttle 400 --ignore-crashes --ignore-timeouts -v 80 > "$OUT/monkey.txt" 2>&1
sleep 3

kill $LOGPID 2>/dev/null
timeout 60 adb logcat -d > "$OUT/logcat_full.txt" 2>&1
timeout 60 adb logcat -d -b crash > "$OUT/logcat_crash.txt" 2>&1
# فقط خطاهای خود اپ (نویزهای سیستم امولاتور مثل managedprovisioning حذف می‌شن)
grep -E "AndroidRuntime|FATAL EXCEPTION|com\.appdony\.app" "$OUT/logcat_full.txt" | grep -E " E | F " > "$OUT/logcat_errors.txt" 2>&1
timeout 60 adb shell dumpsys activity exit-info $PKG > "$OUT/exit-info.txt" 2>&1
adb shell pidof $PKG > "$OUT/pid.txt" 2>&1
timeout 30 adb exec-out screencap -p > "$OUT/screenshot.png" 2>/dev/null
echo "== done" >> "$OUT/steps.txt"
exit 0
