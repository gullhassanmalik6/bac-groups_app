package woyou.aidlservice.jiuiv5;

import woyou.aidlservice.jiuiv5.ICallback;

interface IWoyouService {
    void printerInit(ICallback callback);
    void printText(String text, ICallback callback);
    void printTextWithFont(String text, String typeface, float fontsize, ICallback callback);
    void lineWrap(int n, ICallback callback);
    void cutPaper(ICallback callback);
    String getPrinterSerialNo();
}
