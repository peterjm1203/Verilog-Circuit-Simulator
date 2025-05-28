package backend;

public class DataWrapper<T> {
    T data;
    DataWrapper<T> next;

    DataWrapper(T data) {
        this.data = data;
    }

    void add(T data) {
        if (this.data == null) {
            this.data = data;
        } else {
            DataWrapper<T> wrapper = this;
            while (wrapper.next != null) {
                wrapper = wrapper.next;
            }
            wrapper.next = new DataWrapper<T>(data);
        }
    }

    @Override
    public String toString() {
        StringBuilder nameList = new StringBuilder();
        if (data == null)
            return "";
        DataWrapper<T> dataWrapper = this;
        nameList.append(dataWrapper.data.toString() + " ");
        dataWrapper = dataWrapper.next;
        while (dataWrapper != null) {
            nameList.append(" " + dataWrapper.data.toString());
            dataWrapper = dataWrapper.next;
        }
        return nameList.toString();
    }

    public int count() {
        if (data == null) {
            return 0;
        }
        int count = 0;
        DataWrapper<T> dataWrapper = this;
        while (dataWrapper != null) {
            count++;
            dataWrapper = dataWrapper.next;
        }
        return count;
    }
}
