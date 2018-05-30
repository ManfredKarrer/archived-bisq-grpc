/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.grpc;

import bisq.core.Daemon;

import java.io.IOException;

import java.util.logging.Logger;



import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class BisqGrpcServer {
    private static final Logger logger = Logger.getLogger(BisqGrpcServer.class.getName());

    private static Server server;
    private static Daemon bisqDaemon;

    public static void main(String[] args) throws IOException, InterruptedException {
        final BisqGrpcServer server = new BisqGrpcServer(new Daemon());
        server.start();
        server.blockUntilShutdown();
    }

    private BisqGrpcServer(Daemon bisqDaemon) {
        this.bisqDaemon = bisqDaemon;
    }

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new GetVersionImpl())
                .addService(new GetBalanceImpl())
                .addService(new StopServerImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            BisqGrpcServer.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    private static void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    // Services
    static class GetVersionImpl extends GetVersionGrpc.GetVersionImplBase {
        @Override
        public void getVersion(GetVersionRequest req, StreamObserver<GetVersionReply> responseObserver) {
            GetVersionReply reply = GetVersionReply.newBuilder().setVersion(bisqDaemon.getVersion()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    static class GetBalanceImpl extends GetBalanceGrpc.GetBalanceImplBase {
        @Override
        public void getBalance(GetBalanceRequest req, StreamObserver<GetBalanceReply> responseObserver) {
            GetBalanceReply reply = GetBalanceReply.newBuilder().setBalance(bisqDaemon.getBalance()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    static class StopServerImpl extends StopServerGrpc.StopServerImplBase {
        @Override
        public void stopServer(StopServerRequest req, StreamObserver<StopServerReply> responseObserver) {
            StopServerReply reply = StopServerReply.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

            stop();
        }
    }
}
