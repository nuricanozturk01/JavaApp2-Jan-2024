package org.csystem.app.generator.random.server;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.csystem.generator.random.*;
import org.csystem.util.grpc.error.GrpcErrorUtil;
import org.csystem.util.string.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

@GrpcService
@Scope("prototype")
@Slf4j
public class RandomGeneratorServer extends RandomTextGeneratorServiceGrpc.RandomTextGeneratorServiceImplBase {
    private final RandomGenerator m_randomGenerator;

    @Value("${grpc.server.minCount}")
    private int m_minCount;

    @Value("${grpc.server.maxCount}")
    private int m_maxCount;

    @Value("${grpc.server.chunkSize}")
    private int m_chunkSize;

    private void generateTextsOnNextCallback(StreamObserver<TextInfo> responseObserver, int count)
    {
        try {
            var text = StringUtil.generateRandomTextEN(m_randomGenerator, count);

            log.info("Generated Text:{}", text);

            var textInfo = TextInfo.newBuilder().setText(text).build();

            responseObserver.onNext(textInfo);
            Thread.sleep(300);
        }
        catch (InterruptedException ignore) {

        }
    }

    private void generateMultipleTextsTRResponseCallback(StreamObserver<TextInfo> responseObserver, int count)
    {
        var text = StringUtil.generateRandomTextTR(m_randomGenerator, count);

        log.info("Yazı:{}", text);
        responseObserver.onNext(TextInfo.newBuilder().setText(text).build());
    }

    public RandomGeneratorServer(RandomGenerator randomGenerator)
    {
        m_randomGenerator = randomGenerator;
    }

    @Override
    public void generateTextEN(TextGenerateInfo request, StreamObserver<TextInfo> responseObserver)
    {
        var count = request.getCount();

        log.info("Count:{}", count);

        if (count <= 0) {
            GrpcErrorUtil.invalidArgumentError(responseObserver, "Count must be positive");
            return;
        }

        if (count > m_maxCount) {
            GrpcErrorUtil.outOfRangeError(responseObserver, "Count can not be greater than %d".formatted(m_maxCount));
            return;
        }
        var text = StringUtil.generateRandomTextEN(m_randomGenerator, count);

        log.info("generateRandomTextEN:Count:{}, Text:{}", count, text);

        var response = TextInfo.newBuilder()
                .setText(text).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void generateTextTR(TextGenerateInfo request, StreamObserver<TextInfo> responseObserver)
    {
        var count = request.getCount();

        log.info("Count:{}", count);

        if (count <= 0) {
            GrpcErrorUtil.invalidArgumentError(responseObserver, "Count pozitif olmalıdır");
            return;
        }

        if (count > m_maxCount) {
            GrpcErrorUtil.outOfRangeError(responseObserver, "Count %d sayısından küçük olamaz".formatted(m_maxCount));
            return;
        }
        var text = StringUtil.generateRandomTextTR(m_randomGenerator, count);

        log.info("generateRandomTextTR:Count:{}, Text:{}", count, text);

        var response = TextInfo.newBuilder()
                .setText(text).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void generateTextsEN(TextsGenerateInfo request, StreamObserver<TextInfo> responseObserver)
    {
        var n = request.getN();
        var count = request.getCount();

        log.info("N:{}, Count:{}, ", n, count);

        if (count <= 0) {
            GrpcErrorUtil.invalidArgumentError(responseObserver, "Count must be positive");
            return;
        }

        if (n <= 0) {
            GrpcErrorUtil.invalidArgumentError(responseObserver, "N must be positive");
            return;
        }

        IntStream.range(0, n).forEach(i -> generateTextsOnNextCallback(responseObserver, count));
        responseObserver.onCompleted();
    }

    @Override
    public void generateTextsTR(TextsGenerateInfo request, StreamObserver<TextInfo> responseObserver)
    {
        var n = request.getN();
        var count = request.getCount();

        log.info("N:{}, Count:{}, ", n, count);

        if (count <= 0) {
            GrpcErrorUtil.invalidArgumentError(responseObserver, "Count pozitif olmalıdır");
            return;
        }

        if (n <= 0) {
            GrpcErrorUtil.invalidArgumentError(responseObserver, "N pozitif olmalıdır");
            return;
        }

        IntStream.range(0, n).forEach(i -> generateTextsOnNextCallback(responseObserver, count));
        responseObserver.onCompleted();
    }


    @Override
    public StreamObserver<TextGenerateInfo> generateAndJoinTextsEN(StreamObserver<TextInfo> responseObserver)
    {
        return new StreamObserver<>() {
            final StringBuilder sb = new StringBuilder();

            @Override
            public void onNext(TextGenerateInfo textGenerateInfo)
            {
                var count = textGenerateInfo.getCount();

                if (count <= 0) {
                    GrpcErrorUtil.invalidArgumentError(responseObserver, "Count must be positive");
                    return;
                }

                if (count > m_maxCount) {
                    GrpcErrorUtil.outOfRangeError(responseObserver, "Count can not be greater than %d".formatted(m_maxCount));
                    return;
                }

                var text = StringUtil.generateRandomTextEN(m_randomGenerator, textGenerateInfo.getCount());

                log.info("{}", text);
                sb.append(text);

                if (sb.length() > m_maxCount)
                    GrpcErrorUtil.outOfRangeError(responseObserver, "Text length can not be greater than %d".formatted(m_maxCount));

            }

            @Override
            public void onError(Throwable throwable)
            {
                log.error("Error occurred:{}", throwable.getMessage());
            }

            @Override
            public void onCompleted()
            {
                var text = sb.toString();

                log.info("Completed text:{}", text);

                responseObserver.onNext(TextInfo.newBuilder().setText(text).build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<TextGenerateInfo> generateAndJoinTextsTR(StreamObserver<TextInfo> responseObserver)
    {
        return new StreamObserver<>() {
            final StringBuilder sb = new StringBuilder();

            @Override
            public void onNext(TextGenerateInfo textGenerateInfo)
            {
                var count = textGenerateInfo.getCount();

                if (count <= 0) {
                    GrpcErrorUtil.invalidArgumentError(responseObserver, "Count pozitif olmalıdır");
                    return;
                }

                if (count > m_maxCount) {
                    GrpcErrorUtil.outOfRangeError(responseObserver, "Count fazla %d olmalıdır".formatted(m_maxCount));
                    return;
                }

                var text = StringUtil.generateRandomTextTR(m_randomGenerator, textGenerateInfo.getCount());

                log.info("{}", text);
                sb.append(text);

                if (sb.length() > m_maxCount)
                    GrpcErrorUtil.outOfRangeError(responseObserver, "Yazı uzunluğu en fazla %d olmalıdır".formatted(m_maxCount));

            }

            @Override
            public void onError(Throwable throwable)
            {
                log.error("Hata:{}", throwable.getMessage());
            }

            @Override
            public void onCompleted()
            {
                var text = sb.toString();

                log.info("Yazı:{}", text);

                responseObserver.onNext(TextInfo.newBuilder().setText(text).build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<TextGenerateInfo> generateMultipleTextsEN(StreamObserver<TextInfo> responseObserver)
    {
        return new StreamObserver<>() {
            @Override
            public void onNext(TextGenerateInfo textGenerateInfo)
            {
                var count = textGenerateInfo.getCount();
                var text = StringUtil.generateRandomTextEN(m_randomGenerator, count);

                log.info("Generated Text:{}", text);
                responseObserver.onNext(TextInfo.newBuilder().setText(text).build());
            }

            @Override
            public void onError(Throwable throwable)
            {
                log.error("Error occurred:{}", throwable.getMessage());
            }

            @Override
            public void onCompleted()
            {
                log.info("Request:OnCompleted");
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<TextGenerateInfo> generateMultipleTextsTR(StreamObserver<TextInfo> responseObserver)
    {
        return new StreamObserver<>() {
            final List<Integer> counts = new ArrayList<>();

            @Override
            public void onNext(TextGenerateInfo textGenerateInfo)
            {
                var count = textGenerateInfo.getCount();

                counts.add(count);
            }

            @Override
            public void onError(Throwable throwable)
            {
                log.error("Hata:{}", throwable.getMessage());
            }

            @Override
            public void onCompleted()
            {
                counts.forEach(c -> generateMultipleTextsTRResponseCallback(responseObserver, c));
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void getTextBound(NoParam request, StreamObserver<TextBound> responseObserver)
    {
        var bound = TextBound.newBuilder().setMaxCount(m_maxCount).setMinCount(m_minCount).build();

        responseObserver.onNext(bound);
        responseObserver.onCompleted();
    }

    private boolean createTextsFileEN(int n, int count, String fileName, StreamObserver<Chunk> responseObserver)
    {
        var path = Path.of(fileName);

        if (Files.exists(path)) {
            GrpcErrorUtil.internalError(responseObserver, "File exists!...");
            return false;
        }

        var result = true;

        try (var bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            while (n-- > 0)
                bw.write("%s\r\n".formatted(StringUtil.generateRandomTextEN(m_randomGenerator, count)));
        }
        catch (IOException ex) {
            GrpcErrorUtil.internalError(responseObserver, "IO problem");
            result = false;
        }

        return result;

    }

    private boolean sendFile(String fileName, StreamObserver<Chunk> responseObserver)
    {
        var result = true;
        var data = new byte[m_chunkSize];

        try (var bis = new BufferedInputStream(new FileInputStream(fileName))) {
            int read;

            while ((read = bis.read(data)) > 0)
                responseObserver.onNext(Chunk.newBuilder().setData(ByteString.copyFrom(data, 0, read))
                        .setSize(read).build());
        }
        catch (IOException ex) {
            GrpcErrorUtil.internalError(responseObserver, "IO Problem occurred while sending");
            result = false;
        }

        return result;
    }

    @Override
    public void generateTextsFileEN(TextsFileGenerateInfo request, StreamObserver<Chunk> responseObserver)
    {
        var n = request.getN();
        var count = request.getCount();
        var fileName = request.getFileName();

        log.info("N:{}, Count:{}, File Name:{}", n, count, fileName);

        if (count <= 0) {
            GrpcErrorUtil.invalidArgumentError(responseObserver, "Count must be positive");
            return;
        }

        if (n <= 0) {
            GrpcErrorUtil.invalidArgumentError(responseObserver, "N must be positive");
            return;
        }

        if (fileName.isEmpty()) {
            GrpcErrorUtil.invalidArgumentError(responseObserver, "fileName can not be blank");
            return;
        }

        if (!createTextsFileEN(n, count, fileName, responseObserver)) //Burada demo olarak fileName doğrudan verilmiştir
            return;


        if (!sendFile(fileName, responseObserver))
            return;

        responseObserver.onCompleted();
    }

    @Override
    public void generateTextsFileTR(TextsFileGenerateInfo request, StreamObserver<Chunk> responseObserver)
    {
        throw new UnsupportedOperationException("Not yet implemented!...");
    }

    @Override
    public void generateInt32(Int32GenerateInfo request, StreamObserver<Int32Result> responseObserver)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void generateDouble(DoubleGenerateInfo request, StreamObserver<DoubleResult> responseObserver)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void generateInt64(Int64GenerateInfo request, StreamObserver<Int64Result> responseObserver)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void generateInt32s(Int32sGenerateInfo request, StreamObserver<Int32Result> responseObserver)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void generateDoubles(DoublesGenerateInfo request, StreamObserver<DoubleResult> responseObserver)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void generateInt64s(Int64sGenerateInfo request, StreamObserver<Int64Result> responseObserver)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
