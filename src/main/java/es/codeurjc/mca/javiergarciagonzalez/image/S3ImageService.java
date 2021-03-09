package es.codeurjc.mca.javiergarciagonzalez.image;

import java.io.File;

import javax.annotation.PostConstruct;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service("storageService")
@Profile("production")
public class S3ImageService implements ImageService {

    public static AmazonS3 s3;

    @Value("${amazon.s3.region}")
    private String region;

    @Value("${amazon.s3.bucket-name}")
    private String bucketName;

    @Value("${amazon.s3.endpoint}")
    private String endpoint;

    @PostConstruct
    public void init() {
        s3 = AmazonS3ClientBuilder.standard().withRegion(region).build();

        if(!s3.doesBucketExistV2(bucketName)) {
            s3.createBucket(bucketName);
        }
    }

    @Override
    public String createImage(MultipartFile multiPartFile) {
        String fileName = multiPartFile.getOriginalFilename();
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + fileName);
        
        try {
            multiPartFile.transferTo(file);
            PutObjectRequest por = new PutObjectRequest(bucketName, fileName, file);
            por.withCannedAcl(CannedAccessControlList.PublicRead);
            s3.putObject(por);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image could not been saved", e);
        }


        return s3.getUrl(bucketName, fileName).toString();
    }

    @Override
    public void deleteImage(String image) {

        String imageName = image.replace(endpoint, "");

        if (!s3.doesObjectExist(bucketName, imageName)) {
            throw new AmazonS3Exception("Object to delete not found.");
        }
        s3.deleteObject(bucketName, imageName);
    }

}