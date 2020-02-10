import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.github.tobato.fastdfs.FdfsClientConfig;
import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.domain.proto.storage.DownloadCallback;
import com.github.tobato.fastdfs.service.FastFileStorageClient;

@RunWith(SpringRunner.class)
@Configuration
@Import(FdfsClientConfig.class) // 导入FastDFS-Client组件
@ActiveProfiles({ "test" })
@SpringBootTest(classes = { FdfsTest.class })
@Component
@EnableAutoConfiguration
public class FdfsTest {

	Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private FastFileStorageClient fastFileStorageClient;

	@Test
	public void test() throws Exception {
		StorePath storePath = upload();
		logger.info("{}:{}", storePath.getGroup(),storePath.getPath());
		logger.info("Content: {}",download(storePath.getGroup(),storePath.getPath()));
		logger.info("MetaData: {}",fastFileStorageClient.getMetadata(storePath.getGroup(), storePath.getPath()) );
	}

	private StorePath upload() throws Exception {
		Set<MetaData> mataData = new HashSet<>();
		mataData.add(new MetaData("author", "anon"));
		mataData.add(new MetaData("description", "test only"));

		File file = new File("d:/temp/gc.log");
		// 上传 （文件上传可不填文件信息，填入null即可）
		StorePath storePath = fastFileStorageClient.uploadFile(new FileInputStream(file), file.length(), file.getName(),
				mataData);
		return storePath;
	}

	private String download(String groupName,String path) throws Exception{
		 InputStream ins =  fastFileStorageClient.downloadFile(groupName, path, new DownloadCallback<InputStream>(){
				@Override
				public InputStream recv(InputStream ins) throws IOException {
					return ins;
				}}) ;
		 String result= IOUtils.toString(ins);
		 ins.close();
		 return result;
	};


}
